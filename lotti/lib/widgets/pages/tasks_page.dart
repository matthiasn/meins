import 'dart:async';

import 'package:flutter/cupertino.dart';
import 'package:flutter/material.dart';
import 'package:glass/glass.dart';
import 'package:lotti/classes/journal_entities.dart';
import 'package:lotti/classes/tag_type_definitions.dart';
import 'package:lotti/database/database.dart';
import 'package:lotti/main.dart';
import 'package:lotti/theme.dart';
import 'package:lotti/utils/platform.dart';
import 'package:lotti/widgets/journal/journal_card.dart';
import 'package:lotti/widgets/journal/tags_search_widget.dart';
import 'package:material_floating_search_bar/material_floating_search_bar.dart';

import 'add/new_task_page.dart';

class TasksPage extends StatefulWidget {
  const TasksPage({
    Key? key,
    this.navigatorKey,
  }) : super(key: key);

  final GlobalKey? navigatorKey;

  @override
  _TasksPageState createState() => _TasksPageState();
}

class FilterBy {
  final String typeName;
  final String name;

  FilterBy({
    required this.typeName,
    required this.name,
  });
}

class _TasksPageState extends State<TasksPage> {
  final JournalDb _db = getIt<JournalDb>();
  late Stream<List<JournalEntity>> stream;
  late Stream<List<ConfigFlag>> configFlagsStream;

  Set<String> types = {'Task'};
  Set<String> tagIds = {};
  StreamController<List<TagEntity>> matchingTagsController =
      StreamController<List<TagEntity>>();
  bool starredEntriesOnly = false;

  @override
  void initState() {
    super.initState();

    configFlagsStream = _db.watchConfigFlags();
    configFlagsStream.listen((List<ConfigFlag> configFlags) {
      resetStream();
    });
    resetStream();
  }

  void resetStream() async {
    Set<String>? entryIds;
    for (String tagId in tagIds) {
      Set<String> entryIdsForTag = (await _db.entryIdsByTagId(tagId)).toSet();
      if (entryIds == null) {
        entryIds = entryIdsForTag;
      } else {
        entryIds = entryIds.intersection(entryIdsForTag);
      }
    }
    setState(() {
      stream = _db.watchTasks(
        ids: entryIds?.toList(),
        starredStatuses: starredEntriesOnly ? [true] : [true, false],
        taskStatuses: ['OPEN', 'STARTED'],
      );
    });
  }

  void addTag(String tagId) {
    setState(() {
      tagIds.add(tagId);
      resetStream();
    });
  }

  void removeTag(String remove) {
    setState(() {
      tagIds.remove(remove);
      resetStream();
    });
  }

  Widget buildFloatingSearchBar() {
    final isPortrait =
        MediaQuery.of(context).orientation == Orientation.portrait;

    double portraitWidth = MediaQuery.of(context).size.width * 0.88;

    return FloatingSearchBar(
      hint: 'Search...',
      scrollPadding: const EdgeInsets.only(top: 16, bottom: 56),
      transitionDuration: const Duration(milliseconds: 800),
      transitionCurve: Curves.easeInOut,
      backgroundColor: AppColors.appBarFgColor,
      queryStyle: const TextStyle(
        fontFamily: 'Lato',
        fontSize: 24,
        fontWeight: FontWeight.w300,
      ),
      hintStyle: const TextStyle(
        fontFamily: 'Lato',
        fontSize: 24,
        fontWeight: FontWeight.w300,
      ),
      physics: const BouncingScrollPhysics(),
      borderRadius: BorderRadius.circular(8.0),
      axisAlignment: isPortrait ? 0.0 : -1.0,
      openAxisAlignment: 0.0,
      margins: EdgeInsets.only(top: 8.0, left: isDesktop ? 12.0 : 0.0),
      width: isPortrait ? portraitWidth : 500,
      onQueryChanged: (query) async {
        List<TagEntity> res = await _db.getMatchingTags(
          query.trim(),
          inactive: true,
        );
        matchingTagsController.add(res);
      },
      transition: SlideFadeFloatingSearchBarTransition(),
      actions: [
        FloatingSearchBarAction.searchToClear(
          showIfClosed: false,
        ),
      ],
      builder: (context, transition) {
        return Padding(
          padding: const EdgeInsets.only(
            top: 2.0,
            bottom: 8.0,
            left: 0.0,
            right: 4.0,
          ),
          child: Column(
            mainAxisSize: MainAxisSize.min,
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Row(
                mainAxisAlignment: MainAxisAlignment.end,
                children: [
                  Text(
                    'Starred: ',
                    style: TextStyle(color: AppColors.entryTextColor),
                  ),
                  CupertinoSwitch(
                    value: starredEntriesOnly,
                    activeColor: AppColors.starredGold,
                    onChanged: (bool value) {
                      setState(() {
                        starredEntriesOnly = value;
                        resetStream();
                      });
                    },
                  ),
                ],
              ),
              SelectedTagsWidget(
                removeTag: removeTag,
                tagIds: tagIds.toList(),
              ),
              StreamBuilder<List<TagEntity>>(
                stream: matchingTagsController.stream,
                builder: (
                  BuildContext context,
                  AsyncSnapshot<List<TagEntity>> snapshot,
                ) {
                  return Column(
                    children: [
                      ...?snapshot.data
                          ?.map((tagEntity) => ListTile(
                                title: Text(
                                  tagEntity.tag,
                                  style: TextStyle(
                                    fontFamily: 'Lato',
                                    color: getTagColor(tagEntity),
                                    fontWeight: FontWeight.normal,
                                    fontSize: 20.0,
                                  ),
                                ),
                                onTap: () {
                                  setState(() {
                                    addTag(tagEntity.id);
                                  });
                                },
                              ))
                          .toList(),
                    ],
                  );
                },
              ),
            ],
          ),
        ).asGlass(
          clipBorderRadius: BorderRadius.circular(8),
          tintColor: AppColors.searchBgColor,
        );
      },
    );
  }

  @override
  Widget build(BuildContext context) {
    return Navigator(
      key: widget.navigatorKey,
      onGenerateRoute: (RouteSettings settings) {
        return MaterialPageRoute(
          settings: settings,
          builder: (BuildContext context) {
            return StreamBuilder<List<JournalEntity>>(
              stream: stream,
              builder: (
                BuildContext context,
                AsyncSnapshot<List<JournalEntity>> snapshot,
              ) {
                if (snapshot.data == null) {
                  return Container();
                } else {
                  List<JournalEntity> items = snapshot.data!;

                  return Stack(
                    children: [
                      Scaffold(
                        backgroundColor: AppColors.bodyBgColor,
                        body: Container(
                          margin: const EdgeInsets.all(8.0),
                          child: ListView(
                            children: [
                              const SizedBox(
                                height: 56,
                              ),
                              ...List.generate(
                                items.length,
                                (int index) {
                                  JournalEntity item = items.elementAt(index);
                                  return item.maybeMap(
                                      journalImage: (JournalImage image) {
                                    return JournalImageCard(
                                      item: image,
                                      index: index,
                                    );
                                  }, orElse: () {
                                    return JournalCard(
                                      item: item,
                                      index: index,
                                    );
                                  });
                                },
                                growable: true,
                              )
                            ],
                          ),
                        ),
                        floatingActionButton: const AddTask(),
                      ),
                      buildFloatingSearchBar(),
                    ],
                  );
                }
              },
            );
          },
        );
      },
    );
  }
}

class AddTask extends StatelessWidget {
  const AddTask({
    Key? key,
  }) : super(key: key);

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: const EdgeInsets.all(4.0),
      child: FloatingActionButton(
        heroTag: 'task',
        child: const Icon(
          Icons.add,
          size: 24,
        ),
        backgroundColor: AppColors.actionColor,
        onPressed: () {
          Navigator.of(context).push(
            MaterialPageRoute(
              builder: (BuildContext context) {
                return const NewTaskPage();
              },
            ),
          );
        },
      ),
    );
  }
}