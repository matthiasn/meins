import 'package:flutter/cupertino.dart';
import 'package:flutter/material.dart';
import 'package:flutter_gen/gen_l10n/app_localizations.dart';
import 'package:lotti/classes/entity_definitions.dart';
import 'package:lotti/classes/journal_entities.dart';
import 'package:lotti/database/database.dart';
import 'package:lotti/get_it.dart';
import 'package:lotti/pages/dashboards/dashboard_page.dart';
import 'package:lotti/themes/theme.dart';
import 'package:lotti/utils/platform.dart';
import 'package:lotti/widgets/app_bar/title_app_bar.dart';
import 'package:lotti/widgets/charts/dashboard_habits_chart.dart';
import 'package:lotti/widgets/charts/utils.dart';

class HabitsTabPage extends StatefulWidget {
  const HabitsTabPage({super.key});

  @override
  State<HabitsTabPage> createState() => _HabitsTabPageState();
}

class _HabitsTabPageState extends State<HabitsTabPage> {
  @override
  void initState() {
    super.initState();
  }

  int timeSpanDays = isDesktop ? 14 : 7;

  @override
  Widget build(BuildContext context) {
    final localizations = AppLocalizations.of(context)!;

    return StreamBuilder<List<HabitDefinition>>(
      stream: getIt<JournalDb>().watchHabitDefinitions(),
      builder: (
        BuildContext context,
        AsyncSnapshot<List<HabitDefinition>> snapshot,
      ) {
        final rangeStart = getStartOfDay(
          DateTime.now().subtract(Duration(days: timeSpanDays - 1)),
        );

        final rangeEnd = getEndOfToday();
        final habitItems = snapshot.data ?? [];

        final landscape =
            MediaQuery.of(context).orientation == Orientation.landscape;

        return StreamBuilder<List<JournalEntity>>(
          stream: getIt<JournalDb>().watchHabitCompletionsInRange(
            rangeStart: getStartOfDay(DateTime.now()),
            rangeEnd: rangeEnd,
          ),
          builder: (context, completionsSnapshot) {
            final completedToday = <String>{};

            completionsSnapshot.data?.forEach((item) {
              if (item is HabitCompletionEntry) {
                completedToday.add(item.data.habitId);
              }
            });

            final openHabits =
                habitItems.where((item) => !completedToday.contains(item.id));

            final openNow = openHabits.where(showHabit);
            final pendingLater = openHabits.where((item) => !showHabit(item));

            final completedHabits =
                habitItems.where((item) => completedToday.contains(item.id));

            return Scaffold(
              appBar: TitleAppBar(
                title: localizations.settingsHabitsTitle,
                showBackButton: false,
              ),
              backgroundColor: styleConfig().negspace,
              body: SingleChildScrollView(
                child: Padding(
                  padding: const EdgeInsets.only(
                    bottom: 10,
                    top: 5,
                  ),
                  child: Column(
                    children: [
                      Center(
                        child: CupertinoSegmentedControl(
                          selectedColor: styleConfig().primaryColor,
                          unselectedColor: styleConfig().negspace,
                          borderColor: styleConfig().primaryColor,
                          groupValue: timeSpanDays,
                          onValueChanged: (int value) {
                            setState(() {
                              timeSpanDays = value;
                            });
                          },
                          children: {
                            7: const DaysSegment('7'),
                            14: const DaysSegment('14'),
                            30: const DaysSegment('30'),
                            90: const DaysSegment('90'),
                            if (isDesktop || landscape)
                              180: const DaysSegment('180'),
                          },
                        ),
                      ),
                      if (openNow.isNotEmpty)
                        Padding(
                          padding: const EdgeInsets.only(top: 20),
                          child: Text(
                            localizations.habitsOpenHeader,
                            style: chartTitleStyle(),
                          ),
                        ),
                      const SizedBox(height: 15),
                      ...openNow.map((habitDefinition) {
                        return HabitChartLine(
                          habitId: habitDefinition.id,
                          rangeStart: rangeStart,
                          rangeEnd: rangeEnd,
                        );
                      }),
                      if (completedHabits.isNotEmpty)
                        Padding(
                          padding: const EdgeInsets.only(top: 20),
                          child: Text(
                            localizations.habitsCompletedHeader,
                            style: chartTitleStyle(),
                          ),
                        ),
                      const SizedBox(height: 15),
                      ...completedHabits.map((habitDefinition) {
                        return HabitChartLine(
                          habitId: habitDefinition.id,
                          rangeStart: rangeStart,
                          rangeEnd: rangeEnd,
                        );
                      }),
                      if (pendingLater.isNotEmpty)
                        Padding(
                          padding: const EdgeInsets.only(top: 20),
                          child: Text(
                            localizations.habitsPendingLaterHeader,
                            style: chartTitleStyle(),
                          ),
                        ),
                      const SizedBox(height: 15),
                      ...pendingLater.map((habitDefinition) {
                        return HabitChartLine(
                          habitId: habitDefinition.id,
                          rangeStart: rangeStart,
                          rangeEnd: rangeEnd,
                        );
                      }),
                    ],
                  ),
                ),
              ),
            );
          },
        );
      },
    );
  }
}