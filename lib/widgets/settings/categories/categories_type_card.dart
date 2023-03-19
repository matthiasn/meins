import 'package:flutter/material.dart';
import 'package:lotti/classes/entity_definitions.dart';
import 'package:lotti/themes/theme.dart';
import 'package:lotti/widgets/journal/entry_tools.dart';
import 'package:lotti/widgets/settings/settings_card.dart';
import 'package:material_design_icons_flutter/material_design_icons_flutter.dart';

class CategoriesTypeCard extends StatelessWidget {
  const CategoriesTypeCard(
    this.categoryDefinition, {
    required this.index,
    super.key,
  });

  final CategoryDefinition categoryDefinition;
  final int index;

  @override
  Widget build(BuildContext context) {
    return SettingsNavCard(
      path: '/settings/categories/${categoryDefinition.id}',
      title: categoryDefinition.name,
      trailing: Row(
        mainAxisSize: MainAxisSize.min,
        children: [
          Visibility(
            visible: fromNullableBool(categoryDefinition.private),
            child: Icon(
              MdiIcons.security,
              color: styleConfig().alarm,
              size: settingsIconSize,
            ),
          ),
        ],
      ),
    );
  }
}
