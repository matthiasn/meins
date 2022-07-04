import 'package:flutter_test/flutter_test.dart';
import 'package:lotti/classes/entity_definitions.dart';
import 'package:lotti/get_it.dart';
import 'package:lotti/themes/themes_service.dart';
import 'package:lotti/widgets/settings/dashboards/dashboard_definition_card.dart';
import 'package:material_design_icons_flutter/material_design_icons_flutter.dart';

import '../../widget_test_utils.dart';

void main() {
  group('DashboardDefinitionCard Widget Tests - ', () {
    setUp(() {
      getIt.registerSingleton<ThemesService>(ThemesService(watch: false));
    });
    tearDown(getIt.reset);

    const testName = 'test dashboard name';
    const testDescription = 'test description';
    final testDateTime = DateTime.fromMillisecondsSinceEpoch(0);

    final testItem = DashboardDefinition(
      description: testDescription,
      createdAt: testDateTime,
      updatedAt: testDateTime,
      vectorClock: null,
      id: 'some-id',
      private: false,
      version: '1',
      lastReviewed: testDateTime,
      active: true,
      items: [],
      name: testName,
    );

    testWidgets('displays test dashboard card', (tester) async {
      await tester.pumpWidget(
        makeTestableWidget(
          DashboardDefinitionCard(
            index: 0,
            dashboard: testItem,
          ),
        ),
      );

      await tester.pumpAndSettle();

      expect(find.text(testName), findsOneWidget);
      expect(find.text(testDescription), findsOneWidget);
      expect(find.byIcon(MdiIcons.security), findsNothing);
    });

    testWidgets('displays test dashboard card with private icon',
        (tester) async {
      await tester.pumpWidget(
        makeTestableWidget(
          DashboardDefinitionCard(
            index: 0,
            dashboard: testItem.copyWith(private: true),
          ),
        ),
      );

      await tester.pumpAndSettle();

      expect(find.text(testName), findsOneWidget);
      expect(find.text(testDescription), findsOneWidget);
      expect(find.byIcon(MdiIcons.security), findsOneWidget);
    });

    testWidgets('displays test dashboard card with private icon & review time',
        (tester) async {
      await tester.pumpWidget(
        makeTestableWidget(
          DashboardDefinitionCard(
            index: 0,
            dashboard: testItem.copyWith(
              private: true,
              reviewAt: DateTime(0, 0, 0, 7),
            ),
          ),
        ),
      );

      await tester.pumpAndSettle();

      expect(find.text(testName), findsOneWidget);
      expect(find.text(testDescription), findsOneWidget);
      expect(find.byIcon(MdiIcons.security), findsOneWidget);

      expect(find.text('07:00'), findsOneWidget);
    });
  });
}