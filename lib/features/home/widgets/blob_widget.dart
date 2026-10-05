import 'dart:math' as math;
import 'package:flutter/material.dart';
import '../../../core/models/blob_state.dart';

class BlobWidget extends StatefulWidget {
  final BlobState blobState;

  const BlobWidget({super.key, required this.blobState});

  @override
  State<BlobWidget> createState() => _BlobWidgetState();
}

class _BlobWidgetState extends State<BlobWidget>
    with SingleTickerProviderStateMixin {
  late AnimationController _controller;

  @override
  void initState() {
    super.initState();
    _controller = AnimationController(
      vsync: this,
      duration: const Duration(seconds: 4),
    )..repeat(reverse: true);
  }

  @override
  void dispose() {
    _controller.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    final disableAnimations = MediaQuery.maybeDisableAnimationsOf(context) ??
        MediaQuery.of(context).disableAnimations;

    return TweenAnimationBuilder<Color?>(
      tween: ColorTween(
        begin: widget.blobState.color,
        end: widget.blobState.color,
      ),
      duration: const Duration(milliseconds: 350),
      curve: Curves.easeOut,
      builder: (context, animatedColor, child) {
        final currentColor = animatedColor ?? widget.blobState.color;

        if (disableAnimations) {
          return CustomPaint(
            size: const Size(180, 180),
            painter: _BlobPainter(
              blobState: widget.blobState,
              progress: 0.0,
              animatedColor: currentColor,
              disableAnimations: true,
            ),
          );
        }

        return AnimatedBuilder(
          animation: _controller,
          builder: (context, child) {
            return CustomPaint(
              size: const Size(180, 180),
              painter: _BlobPainter(
                blobState: widget.blobState,
                progress: _controller.value,
                animatedColor: currentColor,
                disableAnimations: false,
              ),
            );
          },
        );
      },
    );
  }
}

class _BlobPainter extends CustomPainter {
  final BlobState blobState;
  final double progress;
  final Color animatedColor;
  final bool disableAnimations;

  _BlobPainter({
    required this.blobState,
    required this.progress,
    required this.animatedColor,
    required this.disableAnimations,
  });

  @override
  void paint(Canvas canvas, Size size) {
    final rawCenter = Offset(size.width / 2, size.height / 2);
    final baseRadius = size.width / 2.6;

    // Apply motion personalities & vertical float
    double verticalFloat = 0.0;
    double tiltAngle = 0.0;

    if (!disableAnimations) {
      switch (blobState.stage) {
        case BlobStage.calm:
          verticalFloat = math.sin(progress * math.pi * 2) * 4.0;
          break;
        case BlobStage.wobbly:
          verticalFloat = math.sin(progress * math.pi * 3) * 6.0;
          tiltAngle = math.sin(progress * math.pi * 4) * 0.04;
          break;
        case BlobStage.cracked:
          verticalFloat = math.sin(progress * math.pi * 2.5) * 3.0;
          tiltAngle = math.sin(progress * math.pi * 6) * 0.03;
          break;
        case BlobStage.shattered:
          verticalFloat = math.sin(progress * math.pi * 4) * 5.0;
          tiltAngle = math.sin(progress * math.pi * 8) * 0.06;
          break;
      }
    }

    final center = Offset(rawCenter.dx, rawCenter.dy + verticalFloat);

    canvas.save();
    if (tiltAngle != 0.0) {
      canvas.translate(center.dx, center.dy);
      canvas.rotate(tiltAngle);
      canvas.translate(-center.dx, -center.dy);
    }

    final paint = Paint()
      ..color = animatedColor
      ..style = PaintingStyle.fill
      ..isAntiAlias = true;

    final path = Path();
    const pointCount = 8;
    final angleStep = (math.pi * 2) / pointCount;

    final points = <Offset>[];
    for (int i = 0; i < pointCount; i++) {
      final angle = i * angleStep;
      double radiusVariation = 0.0;

      if (!disableAnimations) {
        switch (blobState.stage) {
          case BlobStage.calm:
            // Slow, relaxed breathing motion
            radiusVariation = math.sin(progress * math.pi * 2 + i) * 4.0;
            break;
          case BlobStage.wobbly:
            // Fluid, wobbling wave deformation
            radiusVariation =
                math.sin(progress * math.pi * 4 + i * 1.5) * 11.0;
            break;
          case BlobStage.cracked:
            // Tired, uneven asymmetric motion with small tremors
            radiusVariation =
                math.sin(progress * math.pi * 3 + i * 2.0) * 14.0;
            if (i % 2 == 0) radiusVariation -= 7.0;
            break;
          case BlobStage.shattered:
            // Restrained distressed trembling
            radiusVariation =
                math.sin(progress * math.pi * 5 + i * 2.5) * 18.0;
            if (i % 3 == 0) radiusVariation -= 12.0;
            break;
        }
      }

      final r = baseRadius + radiusVariation;
      final x = center.dx + r * math.cos(angle);
      final y = center.dy + r * math.sin(angle);
      points.add(Offset(x, y));
    }

    if (points.isNotEmpty) {
      path.moveTo(
        (points[0].dx + points[pointCount - 1].dx) / 2,
        (points[0].dy + points[pointCount - 1].dy) / 2,
      );
      for (int i = 0; i < pointCount; i++) {
        final p1 = points[i];
        final p2 = points[(i + 1) % pointCount];
        final mid = Offset((p1.dx + p2.dx) / 2, (p1.dy + p2.dy) / 2);
        path.quadraticBezierTo(p1.dx, p1.dy, mid.dx, mid.dy);
      }
    }

    // Draw main organic blob body
    canvas.drawPath(path, paint);

    // Draw cracks for CRACKED & SHATTERED stages
    if (blobState.stage == BlobStage.cracked ||
        blobState.stage == BlobStage.shattered) {
      final crackPaint = Paint()
        ..color = Colors.white.withValues(alpha: 0.65)
        ..style = PaintingStyle.stroke
        ..strokeWidth = 2.5
        ..strokeCap = StrokeCap.round;

      final crackPath1 = Path()
        ..moveTo(center.dx - 15, center.dy - 35)
        ..lineTo(center.dx + 5, center.dy - 10)
        ..lineTo(center.dx - 8, center.dy + 20);

      final crackPath2 = Path()
        ..moveTo(center.dx + 20, center.dy - 15)
        ..lineTo(center.dx + 5, center.dy + 15)
        ..lineTo(center.dx + 25, center.dy + 35);

      canvas.drawPath(crackPath1, crackPaint);
      canvas.drawPath(crackPath2, crackPaint);
    }

    // Draw floating shards for SHATTERED stage
    if (blobState.stage == BlobStage.shattered) {
      final shardPaint = Paint()
        ..color = animatedColor.withValues(alpha: 0.85)
        ..style = PaintingStyle.fill;

      for (int i = 0; i < 5; i++) {
        final shardProgress = disableAnimations ? 0.0 : progress;
        final shardAngle =
            (i * math.pi * 2 / 5) + (shardProgress * math.pi * 0.5);
        final dist = baseRadius +
            28.0 +
            (disableAnimations
                ? 0.0
                : (math.sin(shardProgress * math.pi * 3 + i) * 5.0));
        final shardCenter = Offset(
          center.dx + dist * math.cos(shardAngle),
          center.dy + dist * math.sin(shardAngle),
        );

        final shardPath = Path()
          ..moveTo(shardCenter.dx, shardCenter.dy - 8)
          ..lineTo(shardCenter.dx + 7, shardCenter.dy + 6)
          ..lineTo(shardCenter.dx - 7, shardCenter.dy + 6)
          ..close();

        canvas.drawPath(shardPath, shardPaint);
      }
    }

    canvas.restore();
  }

  @override
  bool shouldRepaint(covariant _BlobPainter oldDelegate) {
    return oldDelegate.progress != progress ||
        oldDelegate.blobState != blobState ||
        oldDelegate.animatedColor != animatedColor ||
        oldDelegate.disableAnimations != disableAnimations;
  }
}
