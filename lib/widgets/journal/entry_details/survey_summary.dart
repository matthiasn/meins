import 'package:flutter/material.dart';
import 'package:flutter/widgets.dart';
import 'package:lotti/classes/journal_entities.dart';
import 'package:lotti/theme.dart';
import 'package:lotti/widgets/charts/dashboard_survey_chart.dart';
import 'package:lotti/widgets/charts/dashboard_survey_data.dart';
import 'package:lotti/widgets/charts/utils.dart';

class SurveySummary extends StatelessWidget {
  const SurveySummary(
    this.surveyEntry, {
    super.key,
  });

  final SurveyEntry surveyEntry;

  @override
  Widget build(BuildContext context) {
    final surveyKey = surveyEntry.data.taskResult.identifier;
    final chartConfig = surveyTypes[surveyKey];

    return Column(
      mainAxisAlignment: MainAxisAlignment.center,
      children: [
        ...surveyEntry.data.calculatedScores.entries
            .map(
              (mapEntry) => Padding(
                padding: const EdgeInsets.all(4),
                child: Row(
                  children: [
                    Text(
                      '${mapEntry.key}: ',
                      style: TextStyle(
                        fontWeight: FontWeight.w900,
                        fontFamily: 'Lato',
                        color: AppColors.entryTextColor,
                        fontSize: 16,
                      ),
                    ),
                    Text(
                      mapEntry.value.toString(),
                      style: TextStyle(
                        fontWeight: FontWeight.w400,
                        fontSize: 16,
                        color: AppColors.entryTextColor,
                        fontFamily: 'Lato',
                      ),
                    ),
                  ],
                ),
              ),
            )
            .toList(),
        DashboardSurveyChart(
          chartConfig: chartConfig!,
          rangeStart: getRangeStart(context: context),
          rangeEnd: getRangeEnd(),
        ),
      ],
    );
  }
}