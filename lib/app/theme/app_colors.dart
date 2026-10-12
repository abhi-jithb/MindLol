import 'package:flutter/material.dart';

/// MindLol design palette and semantic color tokens.
///
/// Follows project guidelines: minimal, flat, one accent color per screen,
/// deep teal brand foundation with coral accents.
class AppColors {
  // Brand Foundation
  static const Color primaryTeal = Color(0xFF0F6E56);
  static const Color coral = Color(0xFFD85A30);

  // Light Mode Tokens
  static const Color lightBackground = Color(0xFFFAFAF8);
  static const Color lightSurface = Color(0xFFFFFFFF);
  static const Color lightSurfaceContainer = Color(0xFFF4F3EE);
  static const Color lightTextPrimary = Color(0xFF1A1A18);
  static const Color lightTextSecondary = Color(0xFF5F5E5A);
  static const Color lightTextTertiary = Color(0xFF8C8A82);
  static const Color lightBorder = Color(0xFFE4E2D8);
  static const Color lightBorderSubtle = Color(0xFFEDECE4);

  // Dark Mode Tokens
  static const Color darkBackground = Color(0xFF141412);
  static const Color darkSurface = Color(0xFF1F1F1D);
  static const Color darkSurfaceContainer = Color(0xFF272724);
  static const Color darkTextPrimary = Color(0xFFF1EFE8);
  static const Color darkTextSecondary = Color(0xFFB4B2A9);
  static const Color darkTextTertiary = Color(0xFF73726C);
  static const Color darkBorder = Color(0xFF333330);
  static const Color darkBorderSubtle = Color(0xFF2A2A27);

  // Blob Stage Accents
  static const Color stageCalm = Color(0xFF0F6E56);
  static const Color stageWobbly = Color(0xFF2563EB);
  static const Color stageCracked = Color(0xFFD85A30);
  static const Color stageShattered = Color(0xFFE11D48);

  // Semantic Feedback Tokens
  static const Color success = Color(0xFF0F6E56);
  static const Color warning = Color(0xFFD85A30);
  static const Color error = Color(0xFFE11D48);
  static const Color paused = Color(0xFF6B7280);
}
