import 'package:beamer/beamer.dart';
import 'package:flutter/material.dart';
import 'package:flutter_bloc/flutter_bloc.dart';
import 'package:flutter_gen/gen_l10n/app_localizations.dart';
import 'package:flutter_localizations/flutter_localizations.dart';
import 'package:form_builder_validators/localization/l10n.dart';
import 'package:lotti/blocs/audio/player_cubit.dart';
import 'package:lotti/blocs/audio/recorder_cubit.dart';
import 'package:lotti/blocs/sync/outbox_cubit.dart';
import 'package:lotti/blocs/sync/sync_config_cubit.dart';
import 'package:lotti/database/database.dart';
import 'package:lotti/get_it.dart';
import 'package:lotti/pages/settings/outbox/outbox_badge.dart';
import 'package:lotti/services/nav_service.dart';
import 'package:lotti/themes/theme.dart';
import 'package:lotti/widgets/audio/audio_recording_indicator.dart';
import 'package:lotti/widgets/misc/desktop_menu.dart';
import 'package:lotti/widgets/misc/time_recording_indicator.dart';

const iconSize = 30.0;

class AppScreen extends StatefulWidget {
  const AppScreen({super.key});

  @override
  State<AppScreen> createState() => _AppScreenState();
}

class _AppScreenState extends State<AppScreen> {
  final navService = getIt<NavService>();

  @override
  Widget build(BuildContext context) {
    final localizations = AppLocalizations.of(context)!;

    return StreamBuilder<int>(
      stream: navService.getIndexStream(),
      builder: (context, snapshot) {
        final index = snapshot.data ?? 0;
        return Scaffold(
          body: Stack(
            children: [
              IndexedStack(
                index: index,
                children: [
                  Beamer(routerDelegate: navService.habitsDelegate),
                  Beamer(routerDelegate: navService.dashboardsDelegate),
                  Beamer(routerDelegate: navService.journalDelegate),
                  Beamer(routerDelegate: navService.settingsDelegate),
                ],
              ),
              const TimeRecordingIndicator(),
              const Positioned(
                right: 120,
                bottom: 0,
                child: AudioRecordingIndicator(),
              ),
            ],
          ),
          bottomNavigationBar: Theme(
            data: Theme.of(context).copyWith(
              focusColor: Colors.transparent,
              hoverColor: Colors.transparent,
              splashColor: Colors.transparent,
            ),
            child: BottomNavigationBar(
              backgroundColor: styleConfig().negspace,
              unselectedItemColor: styleConfig().primaryTextColor,
              selectedItemColor: styleConfig().primaryColor,
              selectedFontSize: fontSizeSmall,
              elevation: 8,
              unselectedFontSize: fontSizeSmall,
              selectedLabelStyle: const TextStyle(
                height: 2,
                fontWeight: FontWeight.bold,
              ),
              unselectedLabelStyle: const TextStyle(height: 2),
              type: BottomNavigationBarType.fixed,
              currentIndex: index,
              items: [
                BottomNavigationBarItem(
                  icon: const Icon(
                    Icons.checklist_outlined,
                    size: iconSize,
                    semanticLabel: 'Habits Tab',
                  ),
                  activeIcon: Icon(
                    Icons.checklist_outlined,
                    size: iconSize,
                    color: styleConfig().primaryColor,
                    semanticLabel: 'Habits Tab',
                  ),
                  label: localizations.navTabTitleHabits,
                  tooltip: '',
                ),
                BottomNavigationBarItem(
                  icon: const Icon(
                    Icons.insights_outlined,
                    size: iconSize,
                    semanticLabel: 'Dashboards Tab',
                  ),
                  activeIcon: Icon(
                    Icons.insights_outlined,
                    size: iconSize,
                    color: styleConfig().primaryColor,
                    semanticLabel: 'Dashboards Tab',
                  ),
                  label: localizations.navTabTitleInsights,
                  tooltip: '',
                ),
                BottomNavigationBarItem(
                  icon: const Icon(
                    Icons.auto_stories_outlined,
                    size: iconSize,
                    semanticLabel: 'Journal Tab',
                  ),
                  activeIcon: Icon(
                    Icons.auto_stories_outlined,
                    size: iconSize,
                    color: styleConfig().primaryColor,
                    semanticLabel: 'Journal Tab',
                  ),
                  label: localizations.navTabTitleJournal,
                  tooltip: '',
                ),
                BottomNavigationBarItem(
                  icon: OutboxBadgeIcon(
                    icon: const Icon(
                      Icons.settings_outlined,
                      size: iconSize,
                      semanticLabel: 'Settings Tab',
                    ),
                  ),
                  activeIcon: OutboxBadgeIcon(
                    icon: Icon(
                      Icons.settings_outlined,
                      size: iconSize,
                      color: styleConfig().primaryColor,
                      semanticLabel: 'Settings Tab',
                    ),
                  ),
                  label: localizations.navTabTitleSettings,
                  tooltip: '',
                ),
              ],
              onTap: navService.tapIndex,
            ),
          ),
        );
      },
    );
  }
}

class MyBeamerApp extends StatelessWidget {
  MyBeamerApp({super.key});

  final JournalDb _db = getIt<JournalDb>();

  final routerDelegate = BeamerDelegate(
    initialPath: getIt<NavService>().currentPath,
    locationBuilder: RoutesLocationBuilder(
      routes: {'*': (context, state, data) => const AppScreen()},
    ).call,
  );

  @override
  Widget build(BuildContext context) {
    return StreamBuilder<Set<String>>(
      stream: _db.watchActiveConfigFlagNames(),
      builder: (context, snapshot) {
        return MultiBlocProvider(
          providers: [
            BlocProvider<SyncConfigCubit>(
              lazy: false,
              create: (BuildContext context) => SyncConfigCubit(
                testOnNetworkChange: true,
              ),
            ),
            BlocProvider<OutboxCubit>(
              lazy: false,
              create: (BuildContext context) => OutboxCubit(),
            ),
            BlocProvider<AudioRecorderCubit>(
              create: (BuildContext context) => AudioRecorderCubit(),
            ),
            BlocProvider<AudioPlayerCubit>(
              create: (BuildContext context) => AudioPlayerCubit(),
            ),
          ],
          child: DesktopMenuWrapper(
            child: MaterialApp.router(
              color: styleConfig().negspace,
              supportedLocales: AppLocalizations.supportedLocales,
              theme: getTheme(),
              localizationsDelegates: const [
                AppLocalizations.delegate,
                FormBuilderLocalizations.delegate,
                GlobalMaterialLocalizations.delegate,
                GlobalWidgetsLocalizations.delegate,
                GlobalCupertinoLocalizations.delegate,
              ],
              debugShowCheckedModeBanner: false,
              routerDelegate: routerDelegate,
              routeInformationParser: BeamerParser(),
              backButtonDispatcher: BeamerBackButtonDispatcher(
                delegate: routerDelegate,
              ),
            ),
          ),
        );
      },
    );
  }
}
