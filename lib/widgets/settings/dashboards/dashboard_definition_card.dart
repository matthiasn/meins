import 'package:auto_route/auto_route.dart';
import 'package:flutter/material.dart';
import 'package:lotti/classes/entity_definitions.dart';
import 'package:lotti/routes/router.gr.dart';
import 'package:lotti/theme.dart';
import 'package:lotti/widgets/journal/entry_tools.dart';
import 'package:material_design_icons_flutter/material_design_icons_flutter.dart';

class DashboardDefinitionCard extends StatelessWidget {
  const DashboardDefinitionCard({
    super.key,
    required this.dashboard,
    required this.index,
  });

  final DashboardDefinition dashboard;
  final int index;

  @override
  Widget build(BuildContext context) {
    final id = dashboard.id;

    return Card(
      color: AppColors.headerBgColor,
      elevation: 8,
      shape: RoundedRectangleBorder(
        borderRadius: BorderRadius.circular(8),
      ),
      child: ListTile(
        contentPadding:
            const EdgeInsets.only(left: 16, top: 8, bottom: 20, right: 16),
        title: Padding(
          padding: const EdgeInsets.symmetric(vertical: 2.0),
          child: Row(
            mainAxisAlignment: MainAxisAlignment.spaceBetween,
            children: [
              Flexible(
                flex: 9,
                child: Text(
                  dashboard.name,
                  style: definitionCardTitleStyle,
                ),
              ),
              const Spacer(),
              Visibility(
                visible: dashboard.private,
                child: Icon(
                  MdiIcons.security,
                  color: AppColors.error,
                  size: definitionCardIconSize,
                ),
              ),
            ],
          ),
        ),
        subtitle: Row(
          mainAxisAlignment: MainAxisAlignment.spaceBetween,
          children: [
            Flexible(
              child: Text(
                dashboard.description,
                style: definitionCardSubtitleStyle,
              ),
            ),
            Text(
              dashboard.reviewAt != null
                  ? hhMmFormat.format(dashboard.reviewAt!)
                  : '',
              style: definitionCardSubtitleStyle,
            ),
          ],
        ),
        onTap: () => context.router.push(EditDashboardRoute(dashboardId: id)),
      ),
    );
  }
}
