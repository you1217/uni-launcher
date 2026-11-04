import 'package:flutter/gestures.dart';
import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:uni_launcher/model/appearance.dart';
import 'package:uni_launcher/theme/design_tokens.dart';
import 'package:uni_launcher/theme/launcher_theme.dart';
import 'package:uni_launcher/widgets/desktop.dart';
import 'package:uni_launcher/widgets/glass_surface.dart';
import 'package:uni_launcher/widgets/page_transition.dart';

void main() {
  testWidgets('GlassSurface omits shadows and blur during motion across all themes',
      (t) async {
    for (final theme in Appearance.themeStyles) {
      final appearance = Appearance(themeStyle: theme, glass: true);
      final style = SurfaceStyle.from(appearance, true);

      Widget buildSurface(bool moving) => MaterialApp(
            theme: launcherTheme(style),
            home: DesktopMotionScope(
              moving: moving,
              child: const GlassSurface(
                shadow: true,
                child: Text('Card content'),
              ),
            ),
          );

      // At rest (moving: false), shadow is present.
      await t.pumpWidget(buildSurface(false));
      final restDecoratedBoxes = t.widgetList<DecoratedBox>(
        find.descendant(
          of: find.byType(GlassSurface),
          matching: find.byType(DecoratedBox),
        ),
      );
      final hasRestShadow = restDecoratedBoxes.any(
        (box) => box.decoration is BoxDecoration &&
            (box.decoration as BoxDecoration).boxShadow != null &&
            (box.decoration as BoxDecoration).boxShadow!.isNotEmpty,
      );
      expect(hasRestShadow, isTrue,
          reason: 'Theme $theme should have box shadow at rest');

      // In motion (moving: true), shadow must be stripped to protect GPU fillrate on 820A.
      await t.pumpWidget(buildSurface(true));
      final movingDecoratedBoxes = t.widgetList<DecoratedBox>(
        find.descendant(
          of: find.byType(GlassSurface),
          matching: find.byType(DecoratedBox),
        ),
      );
      final hasMovingShadow = movingDecoratedBoxes.any(
        (box) => box.decoration is BoxDecoration &&
            (box.decoration as BoxDecoration).boxShadow != null &&
            (box.decoration as BoxDecoration).boxShadow!.isNotEmpty,
      );
      expect(hasMovingShadow, isFalse,
          reason: 'Theme $theme must omit shadow while moving');

      // In motion, BackdropFilter must also be omitted.
      expect(find.byType(BackdropFilter), findsNothing,
          reason: 'Live blur must be bypassed during motion');
    }
  });

  testWidgets('ImmediateHorizontalDragGestureRecognizer has tight slop and DragStartBehavior.down',
      (t) async {
    final recognizer = ImmediateHorizontalDragGestureRecognizer(slop: Design.px(4));
    addTearDown(recognizer.dispose);

    expect(recognizer.slop, Design.px(4));
    expect(recognizer.dragStartBehavior, DragStartBehavior.down);
    expect(recognizer.gestureSettings?.touchSlop, Design.px(4));
  });
}
