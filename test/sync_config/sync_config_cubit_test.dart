import 'package:bloc_test/bloc_test.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:lotti/blocs/sync/sync_config_cubit.dart';
import 'package:lotti/classes/config.dart';
import 'package:lotti/get_it.dart';
import 'package:lotti/services/sync_config_service.dart';
import 'package:lotti/sync/inbox_service.dart';
import 'package:lotti/sync/outbox.dart';
import 'package:mocktail/mocktail.dart';

const defaultWait = Duration(milliseconds: 10);

const testSharedKey = 'abc123';

final testImapConfig = ImapConfig(
  host: 'host',
  folder: 'folder',
  userName: 'userName',
  password: 'password',
  port: 993,
);

final testSyncConfigNoKey = SyncConfig(
  imapConfig: testImapConfig,
  sharedSecret: '',
);

final testSyncConfigConfigured = SyncConfig(
  imapConfig: testImapConfig,
  sharedSecret: testSharedKey,
);

final testSyncConfigJson = testSyncConfigConfigured.toJson().toString();

void main() {
  TestWidgetsFlutterBinding.ensureInitialized();
  var mock = MockSyncConfigService();

  group('SyncConfigCubit Tests - ', () {
    setUp(() {
      final mockInboxService = MockSyncInboxService();
      final mockOutboxService = MockOutboxService();

      when(mockInboxService.init).thenAnswer((_) async {});
      when(mockOutboxService.init).thenAnswer((_) async {});

      getIt
        ..registerSingleton<OutboxService>(mockOutboxService)
        ..registerSingleton<SyncInboxService>(mockInboxService);
    });
    tearDown(getIt.reset);

    blocTest<SyncConfigCubit, SyncConfigState>(
      'in empty state',
      build: SyncConfigCubit.new,
      setUp: () {
        mock = MockSyncConfigService();
        when(mock.getSharedKey).thenAnswer((_) async {});
        when(mock.getImapConfig).thenAnswer((_) async {});
        getIt.registerSingleton<SyncConfigService>(mock);
      },
      act: (c) => c.emitState(),
      wait: defaultWait,
      expect: () => <SyncConfigState>[SyncConfigState.empty()],
      verify: (c) {
        verify(() => mock.getImapConfig()).called(1);
        verify(() => mock.getSharedKey()).called(1);
      },
    );

    blocTest<SyncConfigCubit, SyncConfigState>(
      'in configured state',
      build: SyncConfigCubit.new,
      setUp: () {
        mock = MockSyncConfigService();
        when(mock.getSharedKey).thenAnswer((_) async => testSharedKey);
        when(mock.getImapConfig).thenAnswer((_) async => testImapConfig);

        when(() => mock.testConnection(testSyncConfigNoKey))
            .thenAnswer((_) async => true);

        getIt.registerSingleton<SyncConfigService>(mock);
      },
      act: (c) => c.emitState(),
      wait: defaultWait,
      expect: () => <SyncConfigState>[
        SyncConfigState.empty(),
        SyncConfigState.imapTesting(imapConfig: testImapConfig),
        SyncConfigState.configured(
          imapConfig: testImapConfig,
          sharedSecret: testSharedKey,
        ),
      ],
      verify: (c) {
        verify(() => mock.getImapConfig()).called(1);
        verify(() => mock.getSharedKey()).called(1);
      },
    );

    blocTest<SyncConfigCubit, SyncConfigState>(
      'in IMAP saved state from configured state after deleting shared key',
      build: SyncConfigCubit.new,
      setUp: () {
        mock = MockSyncConfigService();
        when(mock.getSharedKey).thenAnswer((_) async => testSharedKey);
        when(mock.getImapConfig).thenAnswer((_) async => testImapConfig);

        when(mock.deleteSharedKey).thenAnswer((_) async {
          when(mock.getSharedKey).thenAnswer((_) async => null);
        });

        when(() => mock.testConnection(testSyncConfigNoKey))
            .thenAnswer((_) async => true);

        getIt.registerSingleton<SyncConfigService>(mock);
      },
      act: (c) => c.deleteSharedKey(),
      wait: defaultWait,
      expect: () => <SyncConfigState>[
        SyncConfigState.empty(),
        SyncConfigState.imapTesting(imapConfig: testImapConfig),
        SyncConfigState.imapSaved(imapConfig: testImapConfig),
      ],
      verify: (c) {
        verify(() => mock.getImapConfig()).called(1);
        verify(() => mock.getSharedKey()).called(1);
      },
    );

    blocTest<SyncConfigCubit, SyncConfigState>(
      'in empty state from configured state after deleting imap config',
      build: SyncConfigCubit.new,
      setUp: () {
        mock = MockSyncConfigService();
        when(mock.getSharedKey).thenAnswer((_) async => testSharedKey);
        when(mock.getImapConfig).thenAnswer((_) async => testImapConfig);

        when(mock.deleteImapConfig).thenAnswer((_) async {
          when(mock.getImapConfig).thenAnswer((_) async => null);
        });

        when(() => mock.testConnection(testSyncConfigNoKey))
            .thenAnswer((_) async => true);

        getIt.registerSingleton<SyncConfigService>(mock);
      },
      act: (c) => c.deleteImapConfig(),
      wait: defaultWait,
      expect: () => <SyncConfigState>[
        SyncConfigState.empty(),
      ],
      verify: (c) {
        verify(() => mock.getImapConfig()).called(2);
        verify(() => mock.getSharedKey()).called(2);
      },
    );

    blocTest<SyncConfigCubit, SyncConfigState>(
      'imap config valid state',
      build: SyncConfigCubit.new,
      setUp: () {
        mock = MockSyncConfigService();
        when(mock.getSharedKey).thenAnswer((_) async => null);
        when(mock.getImapConfig).thenAnswer((_) async => testImapConfig);

        when(() => mock.testConnection(testSyncConfigNoKey))
            .thenAnswer((_) async => true);

        getIt.registerSingleton<SyncConfigService>(mock);
      },
      act: (c) => c.emitState(),
      wait: defaultWait,
      expect: () => <SyncConfigState>[
        SyncConfigState.empty(),
        SyncConfigState.imapTesting(imapConfig: testImapConfig),
        SyncConfigState.imapSaved(imapConfig: testImapConfig),
      ],
      verify: (c) {
        verify(() => mock.getImapConfig()).called(1);
        verify(() => mock.getSharedKey()).called(1);
      },
    );

    blocTest<SyncConfigCubit, SyncConfigState>(
      'in invalid state when testing fails',
      build: SyncConfigCubit.new,
      setUp: () {
        mock = MockSyncConfigService();
        when(mock.getSharedKey).thenAnswer((_) async => testSharedKey);
        when(mock.getImapConfig).thenAnswer((_) async => testImapConfig);

        when(() => mock.testConnection(testSyncConfigNoKey))
            .thenAnswer((_) async => false);

        getIt.registerSingleton<SyncConfigService>(mock);
      },
      act: (c) => c.emitState(),
      wait: defaultWait,
      expect: () => <SyncConfigState>[
        SyncConfigState.empty(),
        SyncConfigState.imapTesting(imapConfig: testImapConfig),
        SyncConfigState.imapInvalid(
          imapConfig: testImapConfig,
          errorMessage: 'Error',
        ),
      ],
      verify: (c) {
        verify(() => mock.getImapConfig()).called(1);
        verify(() => mock.getSharedKey()).called(1);
      },
    );

    blocTest<SyncConfigCubit, SyncConfigState>(
      'in invalid state after testing incorrect config',
      build: SyncConfigCubit.new,
      setUp: () {
        mock = MockSyncConfigService();
        when(mock.getSharedKey).thenAnswer((_) async => null);
        when(mock.getImapConfig).thenAnswer((_) async => null);

        when(() => mock.testConnection(testSyncConfigNoKey))
            .thenAnswer((_) async {
          when(mock.getImapConfig).thenAnswer((_) async => testImapConfig);
          return false;
        });

        getIt.registerSingleton<SyncConfigService>(mock);
      },
      act: (c) => c.testImapConfig(testImapConfig),
      wait: defaultWait,
      // TODO: examine why it is necessary to skip anything
      skip: 2,
      expect: () => <SyncConfigState>[
        SyncConfigState.imapTesting(imapConfig: testImapConfig),
        SyncConfigState.imapInvalid(
          imapConfig: testImapConfig,
          errorMessage: 'Error',
        ),
      ],
      verify: (c) {
        verify(() => mock.getImapConfig()).called(1);
        verify(() => mock.getSharedKey()).called(1);
      },
    );

    blocTest<SyncConfigCubit, SyncConfigState>(
      'in valid state after testing correct config',
      build: SyncConfigCubit.new,
      setUp: () {
        mock = MockSyncConfigService();
        when(mock.getSharedKey).thenAnswer((_) async => null);
        when(mock.getImapConfig).thenAnswer((_) async => null);

        when(() => mock.testConnection(testSyncConfigNoKey))
            .thenAnswer((_) async {
          when(mock.getImapConfig).thenAnswer((_) async => testImapConfig);
          return true;
        });

        getIt.registerSingleton<SyncConfigService>(mock);
      },
      act: (c) => c.testImapConfig(testImapConfig),
      wait: defaultWait,
      // TODO: examine why it is necessary to skip anything
      skip: 2,
      expect: () => <SyncConfigState>[
        SyncConfigState.imapTesting(imapConfig: testImapConfig),
        SyncConfigState.imapSaved(imapConfig: testImapConfig),
      ],
      verify: (c) {
        verify(() => mock.getImapConfig()).called(1);
        verify(() => mock.getSharedKey()).called(1);
      },
    );

    blocTest<SyncConfigCubit, SyncConfigState>(
      'in configured state after imap saved and generating key',
      build: SyncConfigCubit.new,
      setUp: () {
        mock = MockSyncConfigService();
        when(mock.getSharedKey).thenAnswer((_) async => null);
        when(mock.getImapConfig).thenAnswer((_) async => testImapConfig);

        when(() => mock.testConnection(testSyncConfigNoKey))
            .thenAnswer((_) async {
          return true;
        });

        when(() => mock.generateSharedKey()).thenAnswer((_) async {
          when(mock.getSharedKey).thenAnswer((_) async => testSharedKey);
        });

        getIt.registerSingleton<SyncConfigService>(mock);
      },
      act: (c) => c.generateSharedKey(),
      wait: defaultWait,
      expect: () => <SyncConfigState>[
        SyncConfigState.generating(),
        SyncConfigState.loading(),
        SyncConfigState.imapTesting(imapConfig: testImapConfig),
        SyncConfigState.configured(
          imapConfig: testImapConfig,
          sharedSecret: testSharedKey,
        ),
        // TODO: examine why emitted twice
        SyncConfigState.imapTesting(imapConfig: testImapConfig),
        SyncConfigState.configured(
          imapConfig: testImapConfig,
          sharedSecret: testSharedKey,
        ),
      ],
      verify: (c) {
        verify(() => mock.getImapConfig()).called(2);
        verify(() => mock.getSharedKey()).called(2);
      },
    );

    blocTest<SyncConfigCubit, SyncConfigState>(
      'in configured state after empty and setting valid sync config',
      build: SyncConfigCubit.new,
      setUp: () {
        mock = MockSyncConfigService();
        when(mock.getSharedKey).thenAnswer((_) async => null);
        when(mock.getImapConfig).thenAnswer((_) async => null);

        when(() => mock.testConnection(testSyncConfigNoKey))
            .thenAnswer((_) async {
          return true;
        });

        when(() => mock.setSyncConfig(testSyncConfigJson))
            .thenAnswer((_) async {
          when(mock.getSharedKey).thenAnswer((_) async => testSharedKey);
          when(mock.getImapConfig).thenAnswer((_) async => testImapConfig);
        });

        getIt.registerSingleton<SyncConfigService>(mock);
      },
      act: (c) => c.setSyncConfig(testSyncConfigJson),
      wait: defaultWait,
      expect: () => <SyncConfigState>[
        SyncConfigState.imapTesting(imapConfig: testImapConfig),
        SyncConfigState.configured(
          imapConfig: testImapConfig,
          sharedSecret: testSharedKey,
        ),
        // TODO: examine why emitted twice
        SyncConfigState.imapTesting(imapConfig: testImapConfig),
        SyncConfigState.configured(
          imapConfig: testImapConfig,
          sharedSecret: testSharedKey,
        ),
      ],
      verify: (c) {
        verify(() => mock.getImapConfig()).called(2);
        verify(() => mock.getSharedKey()).called(2);
        verify(() => mock.setSyncConfig(testSyncConfigJson)).called(1);
      },
    );
  });
}

class MockSyncConfigService extends Mock implements SyncConfigService {}

class MockSyncInboxService extends Mock implements SyncInboxService {}

class MockOutboxService extends Mock implements OutboxService {}
