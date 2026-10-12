import 'dart:async';
import 'dart:math' as math;
import 'package:flutter/material.dart';
import '../../../core/models/blob_state.dart';

/// Blob mascot widget with living personality, organic breathing,
/// natural blinking, curious glances, and reactive physics.
///
/// Fully adheres to:
/// - Minimal flat design language
/// - Calm, Wobbly, Cracked, and Shattered stage personalities
/// - Reactive animations on count increments and limit adjustments
/// - Respect for reduced-motion accessibility preferences
class BlobWidget extends StatefulWidget {
  final BlobState blobState;
  final int todayCount;
  final int dailyLimit;

  const BlobWidget({
    super.key,
    required this.blobState,
    this.todayCount = 0,
    this.dailyLimit = 20,
  });

  @override
  State<BlobWidget> createState() => _BlobWidgetState();
}

class _BlobWidgetState extends State<BlobWidget> with TickerProviderStateMixin {
  // Idle breathing & organic float
  late AnimationController _idleController;

  // Natural blinking
  late AnimationController _blinkController;
  Timer? _blinkTimer;
  final math.Random _random = math.Random();

  // Reactive interaction animation (squash & stretch, bounce)
  late AnimationController _reactionController;
  late Animation<double> _reactionScaleX;
  late Animation<double> _reactionScaleY;
  late Animation<double> _reactionBounceY;

  // Stage transition color tween
  late AnimationController _colorController;
  late Animation<Color?> _colorAnimation;
  Color _currentColor = Colors.teal;

  // Previous state trackers to detect meaningful events
  int _lastCount = 0;
  int _lastLimit = 20;
  BlobStage _lastStage = BlobStage.calm;

  @override
  void initState() {
    super.initState();
    _lastCount = widget.todayCount;
    _lastLimit = widget.dailyLimit;
    _lastStage = widget.blobState.stage;
    _currentColor = widget.blobState.color;

    // 1. Idle breathing loop (3.6s organic sine cycle)
    _idleController = AnimationController(
      vsync: this,
      duration: const Duration(milliseconds: 3600),
    )..repeat();

    // 2. Blink controller (200ms natural eyelid closure)
    _blinkController = AnimationController(
      vsync: this,
      duration: const Duration(milliseconds: 200),
    );
    _scheduleNextBlink();

    // 3. Reaction controller (450ms spring response to count / limit changes)
    _reactionController = AnimationController(
      vsync: this,
      duration: const Duration(milliseconds: 450),
    );
    _initReactionTweens();

    // 4. Color transition controller (350ms smooth morph)
    _colorController = AnimationController(
      vsync: this,
      duration: const Duration(milliseconds: 350),
    );
    _colorAnimation = ColorTween(
      begin: _currentColor,
      end: widget.blobState.color,
    ).animate(CurvedAnimation(
      parent: _colorController,
      curve: Curves.easeInOutCubic,
    ));
  }

  void _initReactionTweens() {
    // Anticipation squash -> overshoot stretch -> settling spring
    _reactionScaleX = TweenSequence<double>([
      TweenSequenceItem(
        tween: Tween<double>(begin: 1.0, end: 1.08)
            .chain(CurveTween(curve: Curves.easeOutQuad)),
        weight: 30,
      ),
      TweenSequenceItem(
        tween: Tween<double>(begin: 1.08, end: 0.94)
            .chain(CurveTween(curve: Curves.easeInOutQuad)),
        weight: 40,
      ),
      TweenSequenceItem(
        tween: Tween<double>(begin: 0.94, end: 1.0)
            .chain(CurveTween(curve: Curves.easeOutBack)),
        weight: 30,
      ),
    ]).animate(_reactionController);

    _reactionScaleY = TweenSequence<double>([
      TweenSequenceItem(
        tween: Tween<double>(begin: 1.0, end: 0.92)
            .chain(CurveTween(curve: Curves.easeOutQuad)),
        weight: 30,
      ),
      TweenSequenceItem(
        tween: Tween<double>(begin: 0.92, end: 1.07)
            .chain(CurveTween(curve: Curves.easeInOutQuad)),
        weight: 40,
      ),
      TweenSequenceItem(
        tween: Tween<double>(begin: 1.07, end: 1.0)
            .chain(CurveTween(curve: Curves.easeOutBack)),
        weight: 30,
      ),
    ]).animate(_reactionController);

    _reactionBounceY = TweenSequence<double>([
      TweenSequenceItem(
        tween: Tween<double>(begin: 0.0, end: -8.0)
            .chain(CurveTween(curve: Curves.easeOutCubic)),
        weight: 45,
      ),
      TweenSequenceItem(
        tween: Tween<double>(begin: -8.0, end: 0.0)
            .chain(CurveTween(curve: Curves.bounceOut)),
        weight: 55,
      ),
    ]).animate(_reactionController);
  }

  void _scheduleNextBlink() {
    _blinkTimer?.cancel();
    if (!mounted) return;

    // Blinks occur naturally every 3.2 to 4.8 seconds with slight variation
    final delayMs = 3200 + _random.nextInt(1600);
    _blinkTimer = Timer(Duration(milliseconds: delayMs), () {
      if (!mounted) return;
      final disableAnimations = MediaQuery.maybeDisableAnimationsOf(context) ??
          MediaQuery.of(context).disableAnimations;
      if (!disableAnimations) {
        _blinkController.forward().then((_) {
          if (mounted) {
            _blinkController.reverse().then((_) {
              _scheduleNextBlink();
            });
          }
        });
      } else {
        _scheduleNextBlink();
      }
    });
  }

  @override
  void didUpdateWidget(covariant BlobWidget oldWidget) {
    super.didUpdateWidget(oldWidget);

    final disableAnimations = MediaQuery.maybeDisableAnimationsOf(context) ??
        MediaQuery.of(context).disableAnimations;

    // Detect color/stage change
    if (widget.blobState.stage != _lastStage ||
        widget.blobState.color != _currentColor) {
      _lastStage = widget.blobState.stage;
      if (!disableAnimations) {
        _colorAnimation = ColorTween(
          begin: _currentColor,
          end: widget.blobState.color,
        ).animate(CurvedAnimation(
          parent: _colorController,
          curve: Curves.easeInOutCubic,
        ));
        _colorController.forward(from: 0.0).then((_) {
          if (mounted) _currentColor = widget.blobState.color;
        });
      } else {
        _currentColor = widget.blobState.color;
      }
    }

    // Meaningful interaction events: count increments or limit changes
    final countChanged = widget.todayCount != _lastCount && widget.todayCount > 0;
    final limitChanged = widget.dailyLimit != _lastLimit;

    if ((countChanged || limitChanged) && !disableAnimations) {
      _reactionController.forward(from: 0.0);
    }

    _lastCount = widget.todayCount;
    _lastLimit = widget.dailyLimit;
  }

  @override
  void dispose() {
    _blinkTimer?.cancel();
    _idleController.dispose();
    _blinkController.dispose();
    _reactionController.dispose();
    _colorController.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    final disableAnimations = MediaQuery.maybeDisableAnimationsOf(context) ??
        MediaQuery.of(context).disableAnimations;

    if (disableAnimations) {
      return Center(
        child: SizedBox(
          width: 180,
          height: 180,
          child: CustomPaint(
            painter: _BlobPainter(
              blobState: widget.blobState,
              color: widget.blobState.color,
              idleProgress: 0.0,
              blinkProgress: 0.0,
              reactionScaleX: 1.0,
              reactionScaleY: 1.0,
              reactionBounceY: 0.0,
              disableAnimations: true,
            ),
          ),
        ),
      );
    }

    return Center(
      child: SizedBox(
        width: 180,
        height: 180,
        child: AnimatedBuilder(
          animation: Listenable.merge([
            _idleController,
            _blinkController,
            _reactionController,
            _colorController,
          ]),
          builder: (context, child) {
            final activeColor =
                _colorController.isAnimating ? _colorAnimation.value! : _currentColor;

            return CustomPaint(
              painter: _BlobPainter(
                blobState: widget.blobState,
                color: activeColor,
                idleProgress: _idleController.value,
                blinkProgress: _blinkController.value,
                reactionScaleX: _reactionController.isAnimating
                    ? _reactionScaleX.value
                    : 1.0,
                reactionScaleY: _reactionController.isAnimating
                    ? _reactionScaleY.value
                    : 1.0,
                reactionBounceY: _reactionController.isAnimating
                    ? _reactionBounceY.value
                    : 0.0,
                disableAnimations: false,
              ),
            );
          },
        ),
      ),
    );
  }
}

/// Custom painter that draws Blob's organic body, expressive face,
/// natural blinking eyelids, glances, and stage-specific cues.
class _BlobPainter extends CustomPainter {
  final BlobState blobState;
  final Color color;
  final double idleProgress;
  final double blinkProgress;
  final double reactionScaleX;
  final double reactionScaleY;
  final double reactionBounceY;
  final bool disableAnimations;

  _BlobPainter({
    required this.blobState,
    required this.color,
    required this.idleProgress,
    required this.blinkProgress,
    required this.reactionScaleX,
    required this.reactionScaleY,
    required this.reactionBounceY,
    required this.disableAnimations,
  });

  @override
  void paint(Canvas canvas, Size size) {
    final rawCenter = Offset(size.width / 2, size.height / 2);
    final baseRadius = size.width / 2.7;

    // Organic idle breathing & vertical floating
    double verticalFloat = 0.0;
    double tiltAngle = 0.0;
    double breathingScale = 1.0;

    if (!disableAnimations) {
      final t = idleProgress * math.pi * 2;
      switch (blobState.stage) {
        case BlobStage.calm:
          // Peaceful breathing rhythm (3.6s cycle)
          verticalFloat = math.sin(t) * 3.5;
          breathingScale = 1.0 + (math.sin(t) * 0.02);
          break;
        case BlobStage.wobbly:
          // Playful energetic wobble
          verticalFloat = math.sin(t * 1.5) * 5.0;
          tiltAngle = math.sin(t * 2.0) * 0.04;
          breathingScale = 1.0 + (math.sin(t * 2.0) * 0.035);
          break;
        case BlobStage.cracked:
          // Strained, tired, heavy breathing with slight hesitation
          verticalFloat = math.sin(t * 0.8) * 2.5;
          tiltAngle = math.sin(t * 1.2) * 0.02;
          breathingScale = 1.0 + (math.sin(t * 0.8) * 0.015);
          break;
        case BlobStage.shattered:
          // Restrained, comforting soft float
          verticalFloat = math.sin(t * 1.2) * 3.0;
          tiltAngle = math.sin(t * 1.5) * 0.03;
          breathingScale = 1.0 + (math.sin(t * 1.2) * 0.02);
          break;
      }
    }

    final center = Offset(
      rawCenter.dx,
      rawCenter.dy + verticalFloat + reactionBounceY,
    );

    canvas.save();

    // Apply scale & tilt from center
    canvas.translate(center.dx, center.dy);
    if (tiltAngle != 0.0) canvas.rotate(tiltAngle);
    canvas.scale(
      breathingScale * reactionScaleX,
      breathingScale * reactionScaleY,
    );
    canvas.translate(-center.dx, -center.dy);

    // 1. Draw Organic Blob Body
    _drawBody(canvas, center, baseRadius);

    // 2. Draw Stage Cracks (Cracked & Shattered)
    if (blobState.stage == BlobStage.cracked ||
        blobState.stage == BlobStage.shattered) {
      _drawCracks(canvas, center);
    }

    // 3. Draw Orbiting Shards (Shattered stage - reassuring, floating softly)
    if (blobState.stage == BlobStage.shattered) {
      _drawShards(canvas, center, baseRadius);
    }

    // 4. Draw Expressive Face (Eyes, Eyelids, Pupils, Mouth)
    _drawFace(canvas, center);

    canvas.restore();
  }

  void _drawBody(Canvas canvas, Offset center, double baseRadius) {
    final bodyPaint = Paint()
      ..color = color
      ..style = PaintingStyle.fill
      ..isAntiAlias = true;

    final path = Path();
    const pointCount = 8;
    final angleStep = (math.pi * 2) / pointCount;
    final points = <Offset>[];

    final t = disableAnimations ? 0.0 : (idleProgress * math.pi * 2);

    for (int i = 0; i < pointCount; i++) {
      final angle = i * angleStep;
      double radiusDelta = 0.0;

      if (!disableAnimations) {
        switch (blobState.stage) {
          case BlobStage.calm:
            radiusDelta = math.sin(t + i) * 3.0;
            break;
          case BlobStage.wobbly:
            radiusDelta = math.sin(t * 2.0 + i * 1.5) * 8.5;
            break;
          case BlobStage.cracked:
            radiusDelta = math.sin(t * 1.2 + i * 2.0) * 6.0;
            if (i.isEven) radiusDelta -= 4.0;
            break;
          case BlobStage.shattered:
            radiusDelta = math.sin(t * 1.8 + i * 2.5) * 8.0;
            if (i % 3 == 0) radiusDelta -= 6.0;
            break;
        }
      }

      final r = baseRadius + radiusDelta;
      final x = center.dx + r * math.cos(angle);
      final y = center.dy + r * math.sin(angle);
      points.add(Offset(x, y));
    }

    // Connect points with smooth quadratic curves
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

    canvas.drawPath(path, bodyPaint);
  }

  void _drawFace(Canvas canvas, Offset center) {
    // Eye positions relative to center
    const eyeSpacing = 19.0;
    const eyeVerticalOffset = -6.0;
    final leftEyeCenter =
        Offset(center.dx - eyeSpacing, center.dy + eyeVerticalOffset);
    final rightEyeCenter =
        Offset(center.dx + eyeSpacing, center.dy + eyeVerticalOffset);

    // Glance offset (pupils shift gently in Calm stage)
    double glanceX = 0.0;
    double glanceY = 0.0;
    if (!disableAnimations && blobState.stage == BlobStage.calm) {
      final t = idleProgress * math.pi * 2;
      glanceX = math.sin(t) * 1.6;
      glanceY = math.cos(t * 0.5) * 0.8;
    } else if (!disableAnimations && blobState.stage == BlobStage.wobbly) {
      final t = idleProgress * math.pi * 2;
      glanceX = math.sin(t * 2.0) * 2.2;
      glanceY = math.cos(t * 2.0) * 1.0;
    }

    // 1. Draw Eyes (Base White or Dark depending on expression)
    final eyeRadiusX = (blobState.stage == BlobStage.wobbly) ? 8.5 : 7.5;
    final baseEyeRadiusY = (blobState.stage == BlobStage.wobbly) ? 10.5 : 9.5;
    // Blink squashes vertical radius
    final currentEyeRadiusY =
        math.max(1.0, baseEyeRadiusY * (1.0 - blinkProgress));

    final eyePaint = Paint()
      ..color = Colors.white
      ..style = PaintingStyle.fill;

    // Left and Right Eye Whites
    final leftEyeRect = Rect.fromCenter(
      center: leftEyeCenter,
      width: eyeRadiusX * 2,
      height: currentEyeRadiusY * 2,
    );
    final rightEyeRect = Rect.fromCenter(
      center: rightEyeCenter,
      width: eyeRadiusX * 2,
      height: currentEyeRadiusY * 2,
    );

    if (currentEyeRadiusY > 2.0) {
      canvas.drawOval(leftEyeRect, eyePaint);
      canvas.drawOval(rightEyeRect, eyePaint);

      // Pupils
      final pupilRadius = (blobState.stage == BlobStage.wobbly) ? 4.5 : 4.0;
      final pupilPaint = Paint()
        ..color = const Color(0xFF1A1A18)
        ..style = PaintingStyle.fill;

      final leftPupilPos = Offset(
        leftEyeCenter.dx + glanceX,
        leftEyeCenter.dy + glanceY,
      );
      final rightPupilPos = Offset(
        rightEyeCenter.dx + glanceX,
        rightEyeCenter.dy + glanceY,
      );

      canvas.drawCircle(leftPupilPos, pupilRadius, pupilPaint);
      canvas.drawCircle(rightPupilPos, pupilRadius, pupilPaint);

      // Cute eye gleam (white specular catchlight)
      final gleamPaint = Paint()
        ..color = Colors.white
        ..style = PaintingStyle.fill;
      canvas.drawCircle(
        Offset(leftPupilPos.dx - 1.5, leftPupilPos.dy - 1.5),
        1.3,
        gleamPaint,
      );
      canvas.drawCircle(
        Offset(rightPupilPos.dx - 1.5, rightPupilPos.dy - 1.5),
        1.3,
        gleamPaint,
      );

      // Eyelids for Cracked stage (tired, droopy half-lids)
      if (blobState.stage == BlobStage.cracked) {
        final droopPaint = Paint()
          ..color = color
          ..style = PaintingStyle.fill;

        canvas.drawRect(
          Rect.fromLTWH(
            leftEyeCenter.dx - eyeRadiusX - 1,
            leftEyeCenter.dy - baseEyeRadiusY - 1,
            eyeRadiusX * 2 + 2,
            baseEyeRadiusY * 0.95,
          ),
          droopPaint,
        );
        canvas.drawRect(
          Rect.fromLTWH(
            rightEyeCenter.dx - eyeRadiusX - 1,
            rightEyeCenter.dy - baseEyeRadiusY - 1,
            eyeRadiusX * 2 + 2,
            baseEyeRadiusY * 0.95,
          ),
          droopPaint,
        );
      }
    } else {
      // Natural closed blink lines
      final blinkLinePaint = Paint()
        ..color = Colors.white
        ..style = PaintingStyle.stroke
        ..strokeWidth = 2.2
        ..strokeCap = StrokeCap.round;

      canvas.drawLine(
        Offset(leftEyeCenter.dx - 6, leftEyeCenter.dy),
        Offset(leftEyeCenter.dx + 6, leftEyeCenter.dy),
        blinkLinePaint,
      );
      canvas.drawLine(
        Offset(rightEyeCenter.dx - 6, rightEyeCenter.dy),
        Offset(rightEyeCenter.dx + 6, rightEyeCenter.dy),
        blinkLinePaint,
      );
    }

    // 2. Draw Mouth Expression
    _drawMouth(canvas, center);
  }

  void _drawMouth(Canvas canvas, Offset center) {
    final mouthPaint = Paint()
      ..color = Colors.white
      ..style = PaintingStyle.stroke
      ..strokeWidth = 2.2
      ..strokeCap = StrokeCap.round;

    final mouthCenterY = center.dy + 14.0;

    switch (blobState.stage) {
      case BlobStage.calm:
        // Sweet, relaxed gentle smile
        final smilePath = Path()
          ..moveTo(center.dx - 7, mouthCenterY - 1)
          ..quadraticBezierTo(
            center.dx,
            mouthCenterY + 4.5,
            center.dx + 7,
            mouthCenterY - 1,
          );
        canvas.drawPath(smilePath, mouthPaint);
        break;

      case BlobStage.wobbly:
        // Playful curvy wiggle mouth
        final wigglePath = Path()
          ..moveTo(center.dx - 8, mouthCenterY)
          ..quadraticBezierTo(
            center.dx - 3,
            mouthCenterY + 3.5,
            center.dx,
            mouthCenterY,
          )
          ..quadraticBezierTo(
            center.dx + 3,
            mouthCenterY - 3.5,
            center.dx + 8,
            mouthCenterY,
          );
        canvas.drawPath(wigglePath, mouthPaint);
        break;

      case BlobStage.cracked:
        // Tired, slightly strained flat/sigh mouth
        final sighPath = Path()
          ..moveTo(center.dx - 6, mouthCenterY + 2)
          ..quadraticBezierTo(
            center.dx,
            mouthCenterY - 1,
            center.dx + 6,
            mouthCenterY + 2,
          );
        canvas.drawPath(sighPath, mouthPaint);
        break;

      case BlobStage.shattered:
        // Gentle resting small neutral line (supportive recovery, no humiliation)
        canvas.drawLine(
          Offset(center.dx - 6, mouthCenterY + 1),
          Offset(center.dx + 6, mouthCenterY + 1),
          mouthPaint,
        );
        break;
    }
  }

  void _drawCracks(Canvas canvas, Offset center) {
    final crackPaint = Paint()
      ..color = Colors.white.withValues(alpha: 0.65)
      ..style = PaintingStyle.stroke
      ..strokeWidth = 2.2
      ..strokeCap = StrokeCap.round;

    final crackPath1 = Path()
      ..moveTo(center.dx - 18, center.dy - 38)
      ..lineTo(center.dx - 8, center.dy - 20)
      ..lineTo(center.dx - 16, center.dy + 6);

    final crackPath2 = Path()
      ..moveTo(center.dx + 22, center.dy - 25)
      ..lineTo(center.dx + 12, center.dy + 8)
      ..lineTo(center.dx + 28, center.dy + 28);

    canvas.drawPath(crackPath1, crackPaint);
    canvas.drawPath(crackPath2, crackPaint);
  }

  void _drawShards(Canvas canvas, Offset center, double baseRadius) {
    final shardPaint = Paint()
      ..color = color.withValues(alpha: 0.8)
      ..style = PaintingStyle.fill;

    const shardCount = 5;
    for (int i = 0; i < shardCount; i++) {
      final t = disableAnimations ? 0.0 : (idleProgress * math.pi * 2);
      final angle = (i * (math.pi * 2 / shardCount)) + (t * 0.2);
      final distDelta = disableAnimations ? 0.0 : (math.sin(t * 1.5 + i) * 4.0);
      final dist = baseRadius + 22.0 + distDelta;

      final shardCenter = Offset(
        center.dx + dist * math.cos(angle),
        center.dy + dist * math.sin(angle),
      );

      final shardPath = Path()
        ..moveTo(shardCenter.dx, shardCenter.dy - 6)
        ..lineTo(shardCenter.dx + 5, shardCenter.dy + 5)
        ..lineTo(shardCenter.dx - 5, shardCenter.dy + 5)
        ..close();

      canvas.drawPath(shardPath, shardPaint);
    }
  }

  @override
  bool shouldRepaint(covariant _BlobPainter oldDelegate) {
    return oldDelegate.idleProgress != idleProgress ||
        oldDelegate.blinkProgress != blinkProgress ||
        oldDelegate.reactionScaleX != reactionScaleX ||
        oldDelegate.reactionScaleY != reactionScaleY ||
        oldDelegate.reactionBounceY != reactionBounceY ||
        oldDelegate.color != color ||
        oldDelegate.blobState != blobState ||
        oldDelegate.disableAnimations != disableAnimations;
  }
}
