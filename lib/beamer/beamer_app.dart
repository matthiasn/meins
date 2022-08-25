import 'package:beamer/beamer.dart';
import 'package:flutter/material.dart';
import 'package:flutter_bloc/flutter_bloc.dart';
import 'package:flutter_gen/gen_l10n/app_localizations.dart';
import 'package:flutter_localizations/flutter_localizations.dart';
import 'package:form_builder_validators/localization/l10n.dart';
import 'package:lotti/beamer/locations/dashboards_location.dart';
import 'package:lotti/beamer/locations/journal_location.dart';
import 'package:lotti/beamer/locations/settings_location.dart';
import 'package:lotti/beamer/locations/tasks_location.dart';
import 'package:lotti/blocs/audio/player_cubit.dart';
import 'package:lotti/blocs/audio/recorder_cubit.dart';
import 'package:lotti/blocs/nav/nav_cubit.dart';
import 'package:lotti/blocs/nav/nav_state.dart';
import 'package:lotti/blocs/sync/outbox_cubit.dart';
import 'package:lotti/blocs/sync/sync_config_cubit.dart';
import 'package:lotti/database/database.dart';
import 'package:lotti/get_it.dart';
import 'package:lotti/pages/settings/outbox/outbox_badge.dart';
import 'package:lotti/services/nav_service.dart';
import 'package:lotti/themes/theme.dart';
import 'package:lotti/widgets/audio/audio_recording_indicator.dart';
import 'package:lotti/widgets/bottom_nav/flagged_badge_icon.dart';
import 'package:lotti/widgets/bottom_nav/tasks_badge_icon.dart';
import 'package:lotti/widgets/misc/desktop_menu.dart';
import 'package:lotti/widgets/misc/time_recording_indicator.dart';
import 'package:lotti/widgets/theme/theme_config.dart';
import 'package:salomon_bottom_bar/salomon_bottom_bar.dart';

final dashboardsDelegate = BeamerDelegate(
  initialPath: '/dashboards',
  locationBuilder: (routeInformation, _) {
    if (routeInformation.location!.contains('dashboards')) {
      return DashboardsLocation(routeInformation);
    }
    return NotFound(path: routeInformation.location!);
  },
);

final journalDelegate = BeamerDelegate(
  initialPath: '/journal',
  locationBuilder: (routeInformation, _) {
    if (routeInformation.location!.contains('journal')) {
      return JournalLocation(routeInformation);
    }
    return NotFound(path: routeInformation.location!);
  },
);

final tasksDelegate = BeamerDelegate(
  initialPath: '/tasks',
  locationBuilder: (routeInformation, _) {
    if (routeInformation.location!.contains('tasks')) {
      return TasksLocation(routeInformation);
    }
    return NotFound(path: routeInformation.location!);
  },
);

final settingsDelegate = BeamerDelegate(
  initialPath: '/settings',
  locationBuilder: (routeInformation, _) {
    if (routeInformation.location!.contains('settings')) {
      return SettingsLocation(routeInformation);
    }
    return NotFound(path: routeInformation.location!);
  },
);

class AppScreen extends StatelessWidget {
  const AppScreen({super.key});

  @override
  Widget build(BuildContext context) {
    const showTasks = true;
    final localizations = AppLocalizations.of(context)!;

    void changeTab(int index) {
      context.read<NavCubit>().setIndex(index);
    }

    return StreamBuilder<int>(
      stream: getIt<NavService>().getIndexStream(),
      builder: (context, snapshot) {
        return BlocBuilder<NavCubit, NavState>(
          builder: (
            context,
            NavState state,
          ) {
            final index = snapshot.data ?? 0;
            return Scaffold(
              body: Stack(
                children: [
                  IndexedStack(
                    index: index,
                    children: [
                      Beamer(routerDelegate: state.beamerDelegates[0]),
                      Beamer(routerDelegate: state.beamerDelegates[1]),
                      Beamer(routerDelegate: state.beamerDelegates[2]),
                      Beamer(routerDelegate: state.beamerDelegates[3]),
                    ],
                  ),
                  const TimeRecordingIndicator(),
                  const AudioRecordingIndicator(),
                ],
              ),
              bottomNavigationBar: SalomonBottomBar(
                unselectedItemColor: colorConfig().bottomNavIconUnselected,
                selectedItemColor: colorConfig().bottomNavIconSelected,
                currentIndex: index,
                items: [
                  SalomonBottomBarItem(
                    icon: const Icon(Icons.dashboard_outlined),
                    title: NavTitle(localizations.navTabTitleInsights),
                  ),
                  SalomonBottomBarItem(
                    icon: FlaggedBadgeIcon(),
                    title: NavTitle(localizations.navTabTitleJournal),
                  ),
                  if (showTasks)
                    SalomonBottomBarItem(
                      icon: TasksBadgeIcon(),
                      title: NavTitle(localizations.navTabTitleTasks),
                    ),
                  SalomonBottomBarItem(
                    icon: OutboxBadgeIcon(
                      icon: const Icon(Icons.settings_outlined),
                    ),
                    title: NavTitle(localizations.navTabTitleSettings),
                  ),
                ],
                onTap: changeTab,
              ),
            );
          },
        );
      },
    );
  }
}

class MyBeamerApp extends StatelessWidget {
  MyBeamerApp({super.key});

  final JournalDb _db = getIt<JournalDb>();

  final routerDelegate = BeamerDelegate(
    initialPath: '/dashboards',
    locationBuilder: RoutesLocationBuilder(
      routes: {'*': (context, state, data) => const AppScreen()},
    ),
  );

  @override
  Widget build(BuildContext context) {
    final theme = ThemeData(
      primarySwatch: Colors.grey,
      scaffoldBackgroundColor: colorConfig().bodyBgColor,
      appBarTheme: AppBarTheme(
        iconTheme: IconThemeData(color: colorConfig().entryTextColor),
      ),
    );

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
            BlocProvider<NavCubit>(
              create: (BuildContext context) => NavCubit(
                index: 0,
                path: '/dashboards',
                beamerDelegates: [
                  dashboardsDelegate,
                  journalDelegate,
                  tasksDelegate,
                  settingsDelegate,
                ],
              ),
            ),
          ],
          child: DesktopMenuWrapper(
            ThemeConfigWrapper(
              MaterialApp.router(
                color: colorConfig().bodyBgColor,
                supportedLocales: AppLocalizations.supportedLocales,
                theme: theme,
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
          ),
        );
      },
    );
  }
}

class NavTitle extends StatelessWidget {
  const NavTitle(this.title, {super.key});

  final String title;
  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: const EdgeInsets.only(left: 4),
      child: Text(title),
    );
  }
}
