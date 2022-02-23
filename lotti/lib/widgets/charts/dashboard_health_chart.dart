import 'dart:core';

import 'package:charts_flutter/flutter.dart' as charts;
import 'package:flutter/material.dart';
import 'package:lotti/classes/entity_definitions.dart';
import 'package:lotti/classes/journal_entities.dart';
import 'package:lotti/database/database.dart';
import 'package:lotti/get_it.dart';
import 'package:lotti/theme.dart';

enum HealthChartType {
  lineChart,
  barChart,
}

enum HealthAggregationType {
  none,
  dailySum,
  dailyMax,
}

class HealthTypeConfig {
  final HealthChartType chartType;
  final HealthAggregationType aggregationType;
  final String displayName;

  HealthTypeConfig({
    required this.displayName,
    required this.chartType,
    required this.aggregationType,
  });
}

Map<String, HealthTypeConfig> healthTypes = {
  'HealthDataType.WEIGHT': HealthTypeConfig(
    displayName: 'Weight',
    chartType: HealthChartType.lineChart,
    aggregationType: HealthAggregationType.none,
  ),
  'HealthDataType.RESTING_HEART_RATE': HealthTypeConfig(
    displayName: 'Resting Heart Rate',
    chartType: HealthChartType.lineChart,
    aggregationType: HealthAggregationType.none,
  ),
  'HealthDataType.HEART_RATE_VARIABILITY_SDNN': HealthTypeConfig(
    displayName: 'Heart Rate Variability',
    chartType: HealthChartType.lineChart,
    aggregationType: HealthAggregationType.none,
  ),
  'HealthDataType.BLOOD_PRESSURE_SYSTOLIC': HealthTypeConfig(
    displayName: 'Systolic Blood Pressure',
    chartType: HealthChartType.lineChart,
    aggregationType: HealthAggregationType.none,
  ),
  'HealthDataType.BLOOD_PRESSURE_DIASTOLIC': HealthTypeConfig(
    displayName: 'Diastolic Blood Pressure',
    chartType: HealthChartType.lineChart,
    aggregationType: HealthAggregationType.none,
  ),
};

class Observation {
  final DateTime dateTime;
  final num value;

  Observation(this.dateTime, this.value);

  @override
  String toString() {
    return '$dateTime $value';
  }
}

List<Observation> aggregateNone(List<JournalEntity?> entities) {
  List<Observation> aggregated = [];

  for (JournalEntity? entity in entities) {
    entity?.maybeMap(
      quantitative: (QuantitativeEntry quant) {
        aggregated.add(Observation(
          quant.data.dateFrom,
          quant.data.value,
        ));
      },
      orElse: () {},
    );
  }

  return aggregated;
}

class DashboardHealthChart extends StatelessWidget {
  final DashboardHealthItem chartConfig;

  DashboardHealthChart({
    Key? key,
    required this.chartConfig,
    required this.durationDays,
  }) : super(key: key);

  final JournalDb _db = getIt<JournalDb>();
  final int durationDays;

  @override
  Widget build(BuildContext context) {
    final Duration duration = Duration(days: durationDays);
    final DateTime from = DateTime.now().subtract(duration);

    return StreamBuilder<List<JournalEntity?>>(
      stream: _db.watchQuantitativeByType(
        chartConfig.healthType,
        from,
      ),
      builder: (
        BuildContext context,
        AsyncSnapshot<List<JournalEntity?>> snapshot,
      ) {
        List<JournalEntity?>? items = snapshot.data;

        if (items == null || items.isEmpty) {
          return const SizedBox.shrink();
        }

        List<charts.Series<Observation, DateTime>> seriesList = [
          charts.Series<Observation, DateTime>(
            id: chartConfig.healthType,
            colorFn: (Observation val, _) {
              return charts.MaterialPalette.blue.shadeDefault;
            },
            domainFn: (Observation val, _) => val.dateTime,
            measureFn: (Observation val, _) => val.value,
            data: aggregateNone(items),
          )
        ];
        return Padding(
          padding: const EdgeInsets.only(bottom: 8),
          child: ClipRRect(
            borderRadius: BorderRadius.circular(16),
            child: Container(
              key: Key('${chartConfig.hashCode}'),
              color: Colors.white,
              height: 160,
              padding: const EdgeInsets.all(8.0),
              child: Column(
                children: [
                  Text(
                    healthTypes[chartConfig.healthType]?.displayName ??
                        chartConfig.healthType,
                    style: chartTitleStyle,
                  ),
                  Expanded(
                    child: charts.TimeSeriesChart(
                      seriesList,
                      animate: true,
                      behaviors: [
                        charts.RangeAnnotation([
                          charts.RangeAnnotationSegment(
                            from,
                            DateTime.now(),
                            charts.RangeAnnotationAxisType.domain,
                          ),
                        ]),
                      ],
                      domainAxis: charts.DateTimeAxisSpec(
                        tickProviderSpec: charts.AutoDateTimeTickProviderSpec(),
                      ),
                      defaultRenderer: charts.LineRendererConfig<DateTime>(
                        includePoints: true,
                        strokeWidthPx: 2.5,
                        radiusPx: 4,
                      ),
                      primaryMeasureAxis: const charts.NumericAxisSpec(
                        tickProviderSpec: charts.BasicNumericTickProviderSpec(
                          zeroBound: false,
                          desiredTickCount: 5,
                        ),
                      ),
                    ),
                  ),
                ],
              ),
            ),
          ),
        );
      },
    );
  }
}
