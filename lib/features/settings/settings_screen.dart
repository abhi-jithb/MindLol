import 'package:flutter/material.dart';
import '../../app/theme/app_colors.dart';
import '../../core/platform/mindlol_platform.dart';
import '../../core/platform/mindlol_status.dart';
import '../home/widgets/limit_edit_modal.dart';

class SettingsScreen extends StatefulWidget {
  final MindLolPlatform platform;
  final MindLolStatus currentStatus;

  const SettingsScreen({
    super.key,
    required this.platform,
    required this.currentStatus,
  });

  @override
  State<SettingsScreen> createState() => _SettingsScreenState();
}

class _SettingsScreenState extends State<SettingsScreen> {
  late MindLolStatus _status;

  @override
  void initState() {
    super.initState();
    _status = widget.currentStatus;
  }

  Future<void> _updateLimit(int newLimit) async {
    try {
      final updated = await widget.platform.setDailyLimit(newLimit);
      if (mounted) {
        setState(() => _status = updated);
      }
    } catch (_) {}
  }

  Future<void> _toggleApp({bool? instagram, bool? youtube}) async {
    try {
      final updated = await widget.platform.setAppConfiguration(
        instagramEnabled: instagram,
        youtubeEnabled: youtube,
      );
      if (mounted) {
        setState(() => _status = updated);
      }
    } catch (_) {}
  }

  void _openLimitModal() {
    LimitEditModal.show(
      context,
      currentLimit: _status.dailyLimit,
      onSave: (newLimit) => _updateLimit(newLimit),
    );
  }

  void _showPrivacyDialog() {
    showDialog(
      context: context,
      builder: (context) => AlertDialog(
        title: const Text('Privacy & Security'),
        content: const Text(
          'MindLol is privacy-first by design. All detection and counting run locally on your device.\n\n'
          'MindLol NEVER inspects, stores, or transmits video content, URLs, creator usernames, captions, or screenshots.',
        ),
        actions: [
          TextButton(
            onPressed: () => Navigator.of(context).pop(),
            child: const Text('Got it'),
          ),
        ],
      ),
    );
  }

  @override
  Widget build(BuildContext context) {
    final isDark = Theme.of(context).brightness == Brightness.dark;
    final primaryTextColor =
        isDark ? AppColors.darkTextPrimary : AppColors.lightTextPrimary;
    final secondaryTextColor =
        isDark ? AppColors.darkTextSecondary : AppColors.lightTextSecondary;

    return Scaffold(
      appBar: AppBar(
        title: const Text(
          'Settings',
          style: TextStyle(fontWeight: FontWeight.bold),
        ),
        centerTitle: true,
        elevation: 0,
      ),
      body: SafeArea(
        child: ListView(
          padding: const EdgeInsets.symmetric(horizontal: 20, vertical: 16),
          children: [
            _buildSectionHeader('USAGE', secondaryTextColor),
            const SizedBox(height: 8),
            Card(
              child: ListTile(
                title: Text(
                  'Daily limit',
                  style: TextStyle(
                    fontWeight: FontWeight.w600,
                    color: primaryTextColor,
                  ),
                ),
                subtitle: Text(
                  '${_status.dailyLimit} Reels',
                  style: TextStyle(color: secondaryTextColor),
                ),
                trailing: const Icon(Icons.chevron_right_rounded),
                onTap: _openLimitModal,
              ),
            ),

            const SizedBox(height: 24),
            _buildSectionHeader('SUPPORTED APPS', secondaryTextColor),
            const SizedBox(height: 8),
            Card(
              child: Column(
                children: [
                  SwitchListTile(
                    title: Text(
                      'Instagram Reels',
                      style: TextStyle(
                        fontWeight: FontWeight.w600,
                        color: primaryTextColor,
                      ),
                    ),
                    subtitle: Text(
                      'Count and limit Instagram Reels',
                      style: TextStyle(color: secondaryTextColor, fontSize: 13),
                    ),
                    activeTrackColor: AppColors.primaryTeal,
                    value: _status.instagramEnabled,
                    onChanged: (val) => _toggleApp(instagram: val),
                  ),
                  const Divider(height: 1),
                  SwitchListTile(
                    title: Text(
                      'YouTube Shorts',
                      style: TextStyle(
                        fontWeight: FontWeight.w600,
                        color: primaryTextColor,
                      ),
                    ),
                    subtitle: Text(
                      'Count and limit YouTube Shorts',
                      style: TextStyle(color: secondaryTextColor, fontSize: 13),
                    ),
                    activeTrackColor: AppColors.primaryTeal,
                    value: _status.youtubeEnabled,
                    onChanged: (val) => _toggleApp(youtube: val),
                  ),
                ],
              ),
            ),

            const SizedBox(height: 24),
            _buildSectionHeader('BLOCKING', secondaryTextColor),
            const SizedBox(height: 8),
            Card(
              child: ListTile(
                title: Text(
                  'Blocking Mode',
                  style: TextStyle(
                    fontWeight: FontWeight.w600,
                    color: primaryTextColor,
                  ),
                ),
                subtitle: Text(
                  'Block at daily limit',
                  style: TextStyle(color: secondaryTextColor),
                ),
                trailing: const Icon(
                  Icons.check_circle_outline_rounded,
                  color: AppColors.primaryTeal,
                ),
              ),
            ),

            const SizedBox(height: 24),
            _buildSectionHeader('ABOUT', secondaryTextColor),
            const SizedBox(height: 8),
            Card(
              child: Column(
                children: [
                  ListTile(
                    leading: const Icon(Icons.privacy_tip_outlined),
                    title: Text(
                      'Privacy Disclosure',
                      style: TextStyle(
                        fontWeight: FontWeight.w600,
                        color: primaryTextColor,
                      ),
                    ),
                    subtitle: Text(
                      'On-device counting commitment',
                      style: TextStyle(color: secondaryTextColor, fontSize: 13),
                    ),
                    trailing: const Icon(Icons.chevron_right_rounded),
                    onTap: _showPrivacyDialog,
                  ),
                  const Divider(height: 1),
                  ListTile(
                    leading: const Icon(Icons.info_outline_rounded),
                    title: Text(
                      'MindLol Version',
                      style: TextStyle(
                        fontWeight: FontWeight.w600,
                        color: primaryTextColor,
                      ),
                    ),
                    subtitle: Text(
                      'v1.0.0 (Core Foundation)',
                      style: TextStyle(color: secondaryTextColor, fontSize: 13),
                    ),
                  ),
                ],
              ),
            ),
          ],
        ),
      ),
    );
  }

  Widget _buildSectionHeader(String title, Color textColor) {
    return Padding(
      padding: const EdgeInsets.only(left: 4, bottom: 4),
      child: Text(
        title,
        style: TextStyle(
          fontSize: 12,
          fontWeight: FontWeight.bold,
          letterSpacing: 1.0,
          color: textColor,
        ),
      ),
    );
  }
}
