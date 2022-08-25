import 'package:flutter/material.dart';
import 'package:lotti/classes/entity_definitions.dart';
import 'package:lotti/services/nav_service.dart';
import 'package:lotti/themes/theme.dart';
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
    void onTap() => beamToNamed('/settings/dashboards/$id');
    void onTapView() => beamToNamed('/dashboards/$id');

    return Card(
      color: colorConfig().headerBgColor,
      elevation: 8,
      shape: RoundedRectangleBorder(
        borderRadius: BorderRadius.circular(8),
      ),
      child: ListTile(
        contentPadding:
            const EdgeInsets.only(left: 16, top: 8, bottom: 20, right: 16),
        title: Padding(
          padding: const EdgeInsets.symmetric(vertical: 2),
          child: Row(
            mainAxisAlignment: MainAxisAlignment.spaceBetween,
            children: [
              Flexible(
                flex: 9,
                child: Text(
                  dashboard.name,
                  style: definitionCardTitleStyle(),
                ),
              ),
              const Spacer(),
              Row(
                children: [
                  Visibility(
                    visible: dashboard.private,
                    child: Icon(
                      MdiIcons.security,
                      color: colorConfig().error,
                      size: settingsIconSize,
                    ),
                  ),
                  const SizedBox(width: 8),
                  IconButton(
                    onPressed: onTapView,
                    icon: Icon(
                      MdiIcons.eyeOutline,
                      color: colorConfig().entryTextColor,
                      size: settingsIconSize,
                    ),
                  ),
                ],
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
                style: definitionCardSubtitleStyle(),
              ),
            ),
            Text(
              dashboard.reviewAt != null
                  ? hhMmFormat.format(dashboard.reviewAt!)
                  : '',
              style: definitionCardSubtitleStyle(),
            ),
          ],
        ),
        onTap: onTap,
      ),
    );
  }
}
