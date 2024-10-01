import 'package:collection/collection.dart';

/// Returns a String name given an Enum Type
String? fromEnum(Object? e) {
  try {
    return e.toString().split('.').last;
  } catch (e) {
    return null;
  }
}

/// Returns an Enum Type given a String name
T? toEnum<T>(String? key, List<T> values) {
  try {
    return values.firstWhereOrNull((v) => key == fromEnum(v));
  } catch (e) {
    return null;
  }
}

bool? toBool(dynamic s) {
  try {
    if (s == null) return null;
    if (s is bool) return s;
    s = s.toString().trim().toLowerCase();
    if (s == "true") return true;
    if (s == "false") return false;
    return null;
  } catch (e) {
    return null;
  }
}
