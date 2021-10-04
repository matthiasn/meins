import 'package:json_annotation/json_annotation.dart';

part 'audio_note.g.dart';

@JsonSerializable()
class AudioNote {
  final String id;
  final DateTime createdAt;
  final String audioFile;
  final String audioDirectory;
  Duration duration;
  DateTime? updatedAt;
  String? transcript;
  double? latitude;
  double? longitude;
  String? vectorClock;

  AudioNote({
    required this.id,
    required this.createdAt,
    required this.audioFile,
    required this.audioDirectory,
    required this.duration,
    this.updatedAt,
    this.transcript,
    this.latitude,
    this.longitude,
    this.vectorClock,
  });

  factory AudioNote.fromJson(Map<String, dynamic> json) =>
      _$AudioNoteFromJson(json);

  Map<String, dynamic> toJson() => _$AudioNoteToJson(this);

  @override
  String toString() {
    return 'AudioNote{id: $id, created: $createdAt, audioFile: $audioFile}';
  }
}