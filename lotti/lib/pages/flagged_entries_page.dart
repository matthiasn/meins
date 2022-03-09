import 'package:flutter/material.dart';
import 'package:lotti/classes/journal_entities.dart';
import 'package:lotti/database/database.dart';
import 'package:lotti/get_it.dart';
import 'package:lotti/widgets/journal/journal_card.dart';

class FlaggedEntriesPage extends StatefulWidget {
  const FlaggedEntriesPage({
    Key? key,
    this.navigatorKey,
  }) : super(key: key);

  final GlobalKey? navigatorKey;

  @override
  _FlaggedEntriesPageState createState() => _FlaggedEntriesPageState();
}

class _FlaggedEntriesPageState extends State<FlaggedEntriesPage> {
  final JournalDb _db = getIt<JournalDb>();

  late Stream<List<JournalEntity>> stream;

  @override
  void initState() {
    super.initState();
    stream = _db.watchFlaggedImport();
  }

  @override
  Widget build(BuildContext context) {
    return StreamBuilder<List<JournalEntity>>(
      stream: stream,
      builder: (
        BuildContext context,
        AsyncSnapshot<List<JournalEntity>> snapshot,
      ) {
        if (snapshot.data == null) {
          return const SizedBox.shrink();
        } else {
          List<JournalEntity> items = snapshot.data!;

          return Container(
            margin: const EdgeInsets.symmetric(
              vertical: 8.0,
              horizontal: 8.0,
            ),
            child: ListView(
              children: List.generate(
                items.length,
                (int index) {
                  JournalEntity item = items.elementAt(index);
                  return item.maybeMap(journalImage: (JournalImage image) {
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
              ),
            ),
          );
        }
      },
    );
  }
}