import 'dart:async';
import 'dart:io';

import 'package:bloc/bloc.dart';
import 'package:enough_mail/imap/imap_client.dart';
import 'package:enough_mail/imap/message_sequence.dart';
import 'package:enough_mail/imap/response.dart';
import 'package:enough_mail/mail/mail_account.dart';
import 'package:enough_mail/mail/mail_client.dart';
import 'package:enough_mail/mail/mail_events.dart';
import 'package:enough_mail/mail/mail_exception.dart';
import 'package:enough_mail/mime_message.dart';
import 'package:flutter/foundation.dart';
import 'package:flutter_secure_storage/flutter_secure_storage.dart';
import 'package:hydrated_bloc/hydrated_bloc.dart';
import 'package:wisely/blocs/journal/persistence_cubit.dart';
import 'package:wisely/blocs/sync/classes.dart';
import 'package:wisely/blocs/sync/encryption_cubit.dart';
import 'package:wisely/blocs/sync/imap_state.dart';
import 'package:wisely/classes/journal_db_entities.dart';
import 'package:wisely/classes/journal_entities.dart';
import 'package:wisely/classes/sync_message.dart';

import 'imap_tools.dart';

class ImapCubit extends Cubit<ImapState> {
  late final EncryptionCubit _encryptionCubit;
  late final PersistenceCubit _persistenceCubit;
  late final ImapClient _imapClient;
  late final MailClient _mailClient;
  late SyncConfig? _syncConfig;
  late String? _b64Secret;

  final _storage = const FlutterSecureStorage();
  final String sharedSecretKey = 'sharedSecret';
  final String imapConfigKey = 'imapConfig';
  final String lastReadUidKey = 'lastReadUid';

  ImapCubit({
    required EncryptionCubit encryptionCubit,
    required PersistenceCubit persistenceCubit,
  }) : super(ImapState.initial()) {
    _encryptionCubit = encryptionCubit;
    _persistenceCubit = persistenceCubit;
    _imapClient = ImapClient(isLogEnabled: false);
    imapClientInit();
  }

  Future<void> processMessage(MimeMessage message) async {
    if (Platform.isMacOS) {
      String? encryptedMessage = readMessage(message);
      SyncMessage? syncMessage =
          await decryptMessage(encryptedMessage, message, _b64Secret);
      syncMessage?.when(
        journalEntity: (JournalEntity entity, _) async {
          entity.map(
            audioNote: (AudioNote audioNote) async {
              await saveAudioAttachment(message, audioNote, _b64Secret);
            },
            journalImage: (JournalImage journalImage) async {
              debugPrint('processMessage journalImage $journalImage');
              await saveImageAttachment(message, journalImage, _b64Secret);
            },
          );
        },
        journalDbEntity: (JournalDbEntity journalDbEntity) async {
          debugPrint('processMessage inserting ${journalDbEntity.runtimeType}');
          _persistenceCubit.createDbEntity(journalDbEntity, enqueueSync: false);
        },
      );
    } else {
      debugPrint('Ignoring message');
    }
  }

  Future<void> imapClientInit() async {
    SyncConfig? syncConfig = await _encryptionCubit.loadSyncConfig();

    try {
      if (syncConfig != null) {
        _syncConfig = syncConfig;
        _b64Secret = syncConfig.sharedSecret;
        emit(ImapState.loading());
        ImapConfig imapConfig = syncConfig.imapConfig;

        await _imapClient.connectToServer(
          imapConfig.host,
          imapConfig.port,
          isSecure: true,
        );
        emit(ImapState.connected());
        await _imapClient.login(imapConfig.userName, imapConfig.password);
        emit(ImapState.loggedIn());
        await _imapClient.selectInbox();
        await _pollInbox();
        emit(ImapState.online(lastUpdate: DateTime.now()));
        _startPolling();
        _observeInbox();
      }
    } catch (e) {
      emit(ImapState.failed(error: 'failed: $e ${e.toString()}'));
    }
  }

  void _startPolling() async {
    debugPrint('_startPolling');
    Timer.periodic(const Duration(minutes: 1), (timer) async {
      _pollInbox();
    });
  }

  Future<void> _pollInbox() async {
    try {
      debugPrint('_pollInbox');
      if (_syncConfig != null) {
        String? lastReadUidValue = await _storage.read(key: lastReadUidKey);
        int lastReadUid =
            lastReadUidValue != null ? int.parse(lastReadUidValue) : 0;

        var sequence = MessageSequence(isUidSequence: true);
        sequence.addRangeToLast(lastReadUid + 1);
        debugPrint('sequence: $sequence');

        final fetchResult =
            await _imapClient.uidFetchMessages(sequence, 'ENVELOPE');

        List<MimeMessage> messages = fetchResult.messages;

        if (messages.isNotEmpty) {
          MimeMessage oldest = fetchResult.messages.first;
          await _fetchByUid(oldest.uid);
          if (messages.length > 1) {
            await _pollInbox();
          }
        }
        emit(ImapState.online(lastUpdate: DateTime.now()));
      }
    } on MailException catch (e) {
      debugPrint('High level API failed with $e');
      emit(ImapState.failed(error: 'failed: $e ${e.details} ${e.message}'));
    } catch (e) {
      debugPrint('Exception $e');
      emit(ImapState.failed(error: 'failed: $e ${e.toString()}'));
    }
  }

  Future<void> _fetchByUid(int? uid) async {
    if (uid != null) {
      try {
        // odd workaround, prevents intermittent failures on macOS
        await _imapClient.uidFetchMessage(uid, 'BODY.PEEK[]');
        FetchImapResult res =
            await _imapClient.uidFetchMessage(uid, 'BODY.PEEK[]');

        for (final message in res.messages) {
          await processMessage(message);
        }
        await _storage.write(key: lastReadUidKey, value: '$uid');
        emit(ImapState.online(lastUpdate: DateTime.now()));
      } on MailException catch (e) {
        debugPrint('High level API failed with $e');
        emit(ImapState.failed(error: 'failed: $e ${e.details}'));
      }
    }
  }

  Future<void> _observeInbox() async {
    try {
      if (_syncConfig != null) {
        ImapConfig imapConfig = _syncConfig!.imapConfig;
        final account = MailAccount.fromManualSettings(
          'sync',
          imapConfig.userName,
          imapConfig.host,
          imapConfig.host,
          imapConfig.password,
        );

        _mailClient = MailClient(account, isLogEnabled: false);
        await _mailClient.connect();
        final mailboxes =
            await _mailClient.listMailboxesAsTree(createIntermediate: false);
        debugPrint('mailboxes: $mailboxes');
        await _mailClient.selectInbox();
        _mailClient.eventBus
            .on<MailLoadEvent>()
            .listen((MailLoadEvent event) async {
          _pollInbox();
        });
      }
    } on MailException catch (e) {
      debugPrint('High level API failed with $e');
      emit(ImapState.failed(error: 'failed: $e ${e.details}'));
    } catch (e) {
      emit(ImapState.failed(error: 'failed: $e ${e.toString()}'));
    }
  }

  Future<void> resetOffset() async {
    await _storage.delete(key: lastReadUidKey);
  }
}
