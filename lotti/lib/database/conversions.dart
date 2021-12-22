import 'dart:convert';

import 'package:lotti/classes/geolocation.dart';
import 'package:lotti/classes/journal_entities.dart';
import 'package:lotti/classes/measurables.dart';

import 'database.dart';

JournalDbEntity toDbEntity(JournalEntity entity) {
  final DateTime createdAt = entity.meta.createdAt;
  final subtype = entity.maybeMap(
    quantitative: (qd) => qd.data.dataType,
    survey: (SurveyEntry surveyEntry) => surveyEntry.data.taskResult.identifier,
    orElse: () => '',
  );

  Geolocation? geolocation;
  entity.mapOrNull(
    journalAudio: (item) => geolocation = item.geolocation,
    journalImage: (item) => geolocation = item.geolocation,
    journalEntry: (item) => geolocation = item.geolocation,
  );

  String id = entity.meta.id;
  JournalDbEntity dbEntity = JournalDbEntity(
    id: id,
    createdAt: createdAt,
    updatedAt: createdAt,
    dateFrom: entity.meta.dateFrom,
    deleted: entity.meta.deletedAt != null,
    dateTo: entity.meta.dateTo,
    type: entity.runtimeType.toString(),
    subtype: subtype,
    serialized: json.encode(entity),
    schemaVersion: 0,
    longitude: geolocation?.longitude,
    latitude: geolocation?.latitude,
    geohashString: geolocation?.geohashString,
  );

  return dbEntity;
}

JournalEntity fromSerialized(String serialized) {
  return JournalEntity.fromJson(json.decode(serialized));
}

JournalEntity fromDbEntity(JournalDbEntity dbEntity) {
  return fromSerialized(dbEntity.serialized);
}

MeasurableDataType measurableDataType(MeasurableDbEntity dbEntity) {
  return MeasurableDataType.fromJson(json.decode(dbEntity.serialized));
}

MeasurableDbEntity measurableDbEntity(EntityDefinition dataType) {
  return MeasurableDbEntity(
    id: dataType.id,
    uniqueName: dataType.name,
    createdAt: dataType.createdAt,
    updatedAt: dataType.updatedAt,
    serialized: jsonEncode(dataType),
    version: dataType.version,
    status: 0,
  );
}
