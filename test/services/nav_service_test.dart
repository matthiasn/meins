import 'package:flutter_test/flutter_test.dart';
import 'package:lotti/get_it.dart';
import 'package:lotti/services/nav_service.dart';
import 'package:lotti/sync/secure_storage.dart';
import 'package:mocktail/mocktail.dart';

import '../mocks/mocks.dart';

void main() {
  TestWidgetsFlutterBinding.ensureInitialized();

  group('NavService Tests', () {
    setUpAll(() {
      final secureStorageMock = MockSecureStorage();

      when(() => secureStorageMock.readValue(lastRouteKey))
          .thenAnswer((_) async => '/settings');

      when(() => secureStorageMock.writeValue(lastRouteKey, any()))
          .thenAnswer((_) async {});

      getIt
        ..registerSingleton<SecureStorage>(secureStorageMock)
        ..registerSingleton<NavService>(NavService());
    });

    setUp(() {});

    test('', () async {
      final navService = getIt<NavService>();

      expect(navService.index, 0);

      navService.setIndex(1);
      expect(navService.index, 1);

      navService.setIndex(2);
      expect(navService.index, 2);

      navService.setIndex(3);
      expect(navService.index, 3);

      navService.setIndex(0);
      expect(navService.index, 0);

      beamToNamed('/settings');
      expect(navService.index, 3);
      expect(navService.currentPath, '/settings');

      beamToNamed('/settings/advanced');
      expect(navService.index, 3);
      expect(navService.currentPath, '/settings/advanced');

      beamToNamed('/settings/advanced/maintenance');
      expect(navService.index, 3);
      expect(navService.currentPath, '/settings/advanced/maintenance');

      beamToNamed('/tasks');
      expect(navService.index, 2);
      expect(navService.currentPath, '/tasks');

      beamToNamed('/tasks/some-id');
      expect(navService.index, 2);
      expect(navService.currentPath, '/tasks/some-id');

      beamToNamed('/journal');
      expect(navService.index, 1);
      expect(navService.currentPath, '/journal');

      beamToNamed('/dashboards');
      expect(navService.index, 0);
      expect(navService.currentPath, '/dashboards');
    });
  });
}