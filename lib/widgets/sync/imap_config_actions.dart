import 'package:flutter/material.dart';
import 'package:flutter_bloc/flutter_bloc.dart';
import 'package:flutter_gen/gen_l10n/app_localizations.dart';
import 'package:lotti/blocs/sync/sync_config_cubit.dart';
import 'package:lotti/theme.dart';
import 'package:lotti/widgets/misc/buttons.dart';

class ImapConfigActions extends StatelessWidget {
  const ImapConfigActions({Key? key}) : super(key: key);

  @override
  Widget build(BuildContext context) {
    AppLocalizations localizations = AppLocalizations.of(context)!;

    return BlocBuilder<SyncConfigCubit, SyncConfigState>(
        builder: (context, SyncConfigState state) {
      SyncConfigCubit syncConfigCubit = context.read<SyncConfigCubit>();

      return Center(
        child: state.maybeWhen(
          configured: (_, __) => Button(
            localizations.settingsSyncDeleteImapButton,
            onPressed: () {
              syncConfigCubit.deleteImapConfig();
            },
            primaryColor: AppColors.error,
          ),
          imapSaved: (_) => Button(
            localizations.settingsSyncDeleteImapButton,
            onPressed: () {
              syncConfigCubit.deleteImapConfig();
            },
            primaryColor: AppColors.error,
          ),
          imapValid: (_) => Button(
            localizations.settingsSyncSaveButton,
            primaryColor: Colors.blue,
            textColor: AppColors.headerBgColor,
            onPressed: syncConfigCubit.saveImapConfig,
          ),
          imapTesting: (_) => Button(
            localizations.settingsSyncDeleteImapButton,
            onPressed: () {
              syncConfigCubit.deleteImapConfig();
            },
            primaryColor: AppColors.error,
          ),
          imapInvalid: (_, String errorMessage) => Button(
            localizations.settingsSyncDeleteImapButton,
            onPressed: () {
              syncConfigCubit.deleteImapConfig();
            },
            primaryColor: AppColors.error,
          ),
          orElse: () => const SizedBox.shrink(),
        ),
      );
    });
  }
}
