import 'dart:async';

import 'package:flutter/widgets.dart';
import 'package:lotti/get_it.dart';
import 'package:lotti/sync/secure_storage.dart';

const String lastRouteKey = 'LAST_ROUTE_KEY';

class NavService {
  NavService() {
    restoreRoute();
  }

  String? currentRoute;
  List<String> routesByIndex = [];

  Future<void> restoreRoute() async {
    final route = await getSavedRoute();
    currentRoute = route;

    if (route != null) {
      Timer(const Duration(milliseconds: 1), () {
        navigateNamedRoute(route);
        debugPrint('restoreRoute: $route');
      });
    }
  }
}

Future<String?> getSavedRoute() async {
  return await getIt<SecureStorage>().readValue(lastRouteKey);
}

Future<String?> getIdFromSavedRoute() async {
  final regExp = RegExp(
    '[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}',
    caseSensitive: false,
  );
  final route = await getSavedRoute();
  return regExp.firstMatch('$route')?.group(0);
}

void persistNamedRoute(String route) {
  debugPrint('persistNamedRoute: $route');
  getIt<SecureStorage>().writeValue(lastRouteKey, route);
  getIt<NavService>().currentRoute = route;
}

// TODO: remove
void navigateNamedRoute(String route) {
  debugPrint('navigateNamedRoute: $route');
  persistNamedRoute(route);
  // getIt<AppRouter>().navigateNamed(
  //   route,
  //   includePrefixMatches: true,
  //   onFailure: (_) => getIt<AppRouter>().navigateNamed('/'),
  // );
}
