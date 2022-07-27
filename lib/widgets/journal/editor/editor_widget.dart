// ignore_for_file: avoid_dynamic_calls

import 'package:flutter/material.dart';
import 'package:flutter_bloc/flutter_bloc.dart';
import 'package:flutter_quill/flutter_quill.dart' hide Text;
import 'package:lotti/blocs/journal/entry_cubit.dart';
import 'package:lotti/blocs/journal/entry_state.dart';
import 'package:lotti/classes/journal_entities.dart';
import 'package:lotti/themes/theme.dart';
import 'package:lotti/widgets/journal/editor/editor_styles.dart';
import 'package:lotti/widgets/journal/editor/editor_toolbar.dart';

class EditorWidget extends StatelessWidget {
  const EditorWidget({
    super.key,
    this.journalEntity,
    this.minHeight = 40,
    this.maxHeight = double.maxFinite,
    this.padding = 16,
    this.autoFocus = false,
    required this.focusNode,
  });

  final JournalEntity? journalEntity;
  final double maxHeight;
  final double minHeight;
  final bool autoFocus;
  final double padding;
  final FocusNode focusNode;

  @override
  Widget build(BuildContext context) {
    return BlocBuilder<EntryCubit, EntryState>(
      builder: (
        context,
        EntryState snapshot,
      ) {
        final saveFn = context.read<EntryCubit>().save;
        final controller = context.read<EntryCubit>().controller;

        void keyFormatter(
          RawKeyEvent event,
          String char,
          Attribute<dynamic> attribute,
        ) {
          if (event.data.isMetaPressed && event.character == char) {
            if (controller
                .getSelectionStyle()
                .attributes
                .keys
                .contains(attribute.key)) {
              controller.formatSelection(Attribute.clone(attribute, null));
            } else {
              controller.formatSelection(attribute);
            }
          }
        }

        void saveViaKeyboard(RawKeyEvent event) {
          if (event.data.isMetaPressed && event.character == 's') {
            saveFn();
          }
        }

        return RawKeyboardListener(
          focusNode: FocusNode(),
          onKey: (RawKeyEvent event) {
            keyFormatter(event, 'b', Attribute.bold);
            keyFormatter(event, 'i', Attribute.italic);
            saveViaKeyboard(event);
          },
          child: ColoredBox(
            color: colorConfig().editorBgColor,
            child: ConstrainedBox(
              constraints: BoxConstraints(
                maxHeight: maxHeight,
              ),
              child: Column(
                mainAxisSize: MainAxisSize.min,
                children: [
                  ToolbarWidget(
                    id: journalEntity?.meta.id,
                    lastSaved: journalEntity?.meta.updatedAt ??
                        DateTime.fromMillisecondsSinceEpoch(0),
                  ),
                  Flexible(
                    child: Padding(
                      padding: EdgeInsets.symmetric(horizontal: padding),
                      child: QuillEditor(
                        controller: controller,
                        readOnly: false,
                        scrollController: ScrollController(),
                        scrollable: true,
                        focusNode: focusNode,
                        autoFocus: autoFocus,
                        expands: false,
                        minHeight: minHeight,
                        padding: const EdgeInsets.only(top: 8, bottom: 16),
                        keyboardAppearance: Brightness.dark,
                        customStyles: customEditorStyles(
                          textColor: colorConfig().editorTextColor,
                          codeBlockBackground:
                              colorConfig().codeBlockBackground,
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
