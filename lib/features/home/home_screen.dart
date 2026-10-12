import 'dart:async';
import 'package:flutter/material.dart';
import '../../app/theme/app_colors.dart';
import '../../core/models/blob_state.dart';
import '../../core/platform/mindlol_platform.dart';
import '../../core/platform/mindlol_status.dart';
import 'widgets/blob_widget.dart';
import 'widgets/limit_edit_modal.dart';
import 'widgets/permission_banner.dart';
import 'widgets/progress_card.dart';
import '../settings/settings_screen.dart';

class HomeScreen extends StatefulWidget {
  final MindLolPlatform platform;

  const HomeScreen({super.key, required this.platform});

  @override
  State<HomeScreen> createState() => _HomeScreenState();
}

class _HomeScreenState extends State<HomeScreen> with WidgetsBindingObserver {
  MindLolStatus? _status;
  bool _isLoading = true;
  bool _hasError = false;
  StreamSubscription<MindLolStatus>? _subscription;

  @override
  void initState() {
    super.initState();
    WidgetsBinding.instance.addObserver(this);
    _fetchStatus();
    _subscription = widget.platform.statusStream.listen(
      (updatedStatus) {
        if (mounted) {
          setState(() {
            _status = updatedStatus;
            _isLoading = false;
            _hasError = false;
          });
        }
      },
      onError: (err) {
        if (mounted) {
          setState(() {
            _isLoading = false;
            _hasError = true;
          });
        }
      },
    );
  }

  @override
  void dispose() {
    WidgetsBinding.instance.removeObserver(this);
    _subscription?.cancel();
    super.dispose();
  }

  @override
  void didChangeAppLifecycleState(AppLifecycleState state) {
    if (state == AppLifecycleState.resumed) {
      _fetchStatus();
    }
  }

  Future<void> _fetchStatus() async {
    setState(() {
      _isLoading = true;
      _hasError = false;
    });
    try {
      final status = await widget.platform.getMindLolStatus();
      if (mounted) {
        setState(() {
          _status = status;
          _isLoading = false;
          _hasError = false;
        });
      }
    } catch (e) {
      if (mounted) {
        setState(() {
          _isLoading = false;
          _hasError = true;
        });
      }
    }
  }

  Future<void> _updateLimit(int newLimit) async {
    try {
      final updated = await widget.platform.setDailyLimit(newLimit);
      if (mounted) {
        setState(() => _status = updated);
      }
    } catch (e) {
      // Soft failure handling
    }
  }

  void _openLimitEditModal() {
    final currentLimit = _status?.dailyLimit ?? 20;
    LimitEditModal.show(
      context,
      currentLimit: currentLimit,
      onSave: (newLimit) => _updateLimit(newLimit),
    );
  }

  void _openSettings() {
    final currentStatus = _status ??
        const MindLolStatus(
          todayCount: 0,
          dailyLimit: 20,
          limitReached: false,
          accessibilityEnabled: false,
          overlayPermissionGranted: false,
        );

    Navigator.of(context).push(
      MaterialPageRoute(
        builder: (context) => SettingsScreen(
          platform: widget.platform,
          currentStatus: currentStatus,
        ),
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
          'MindLol',
          style: TextStyle(
            fontWeight: FontWeight.bold,
            letterSpacing: 0.5,
          ),
        ),
        centerTitle: true,
        elevation: 0,
        actions: [
          IconButton(
            icon: const Icon(Icons.settings_outlined),
            onPressed: _openSettings,
            tooltip: 'Settings',
          ),
        ],
      ),
      body: SafeArea(
        child: _buildBody(primaryTextColor, secondaryTextColor),
      ),
    );
  }

  Widget _buildBody(Color primaryTextColor, Color secondaryTextColor) {
    if (_isLoading && _status == null) {
      return const Center(
        child: SizedBox(
          width: 32,
          height: 32,
          child: CircularProgressIndicator(
            strokeWidth: 3.0,
            valueColor: AlwaysStoppedAnimation<Color>(AppColors.primaryTeal),
          ),
        ),
      );
    }

    if (_hasError && _status == null) {
      return Center(
        child: Padding(
          padding: const EdgeInsets.symmetric(horizontal: 32),
          child: Column(
            mainAxisAlignment: MainAxisAlignment.center,
            children: [
              const Icon(
                Icons.cloud_off_rounded,
                size: 48,
                color: AppColors.coral,
              ),
              const SizedBox(height: 16),
              Text(
                'MindLol couldn\'t read your usage right now.',
                textAlign: TextAlign.center,
                style: TextStyle(
                  fontSize: 16,
                  fontWeight: FontWeight.w600,
                  color: primaryTextColor,
                ),
              ),
              const SizedBox(height: 20),
              ElevatedButton.icon(
                onPressed: _fetchStatus,
                icon: const Icon(Icons.refresh_rounded, size: 18),
                label: const Text('Try again'),
              ),
            ],
          ),
        ),
      );
    }

    final todayCount = _status?.todayCount ?? 0;
    final dailyLimit = _status?.dailyLimit ?? 20;
    final blobState = BlobState.fromCountAndLimit(todayCount, dailyLimit);
    final limitReached = _status?.limitReached ?? (todayCount >= dailyLimit);

    return RefreshIndicator(
      onRefresh: _fetchStatus,
      color: AppColors.primaryTeal,
      child: SingleChildScrollView(
        physics: const AlwaysScrollableScrollPhysics(),
        padding: const EdgeInsets.symmetric(horizontal: 24, vertical: 16),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.center,
          children: [
            // Setup/Permission Status Banner
            PermissionBanner(
              accessibilityEnabled: _status?.accessibilityEnabled ?? true,
              overlayPermissionGranted:
                  _status?.overlayPermissionGranted ?? true,
              trackingHealthState: _status?.trackingHealthState,
              trackingHealthMessage: _status?.trackingHealthMessage,
              platform: widget.platform,
            ),

            const SizedBox(height: 12),

            // Blob Mascot Centerpiece
            BlobWidget(blobState: blobState),

            const SizedBox(height: 16),

            // Blob Stage Title & Message
            Text(
              blobState.title,
              textAlign: TextAlign.center,
              style: TextStyle(
                fontSize: 22,
                fontWeight: FontWeight.bold,
                color: blobState.color,
              ),
            ),
            const SizedBox(height: 4),
            Text(
              blobState.message,
              textAlign: TextAlign.center,
              style: TextStyle(
                fontSize: 14,
                color: secondaryTextColor,
              ),
            ),

            const SizedBox(height: 28),

            // Primary Metric: Today's Reel Count
            Text(
              '$todayCount',
              style: TextStyle(
                fontSize: 64,
                fontWeight: FontWeight.w800,
                height: 1.0,
                color: primaryTextColor,
              ),
            ),
            const SizedBox(height: 4),
            Text(
              todayCount == 1 ? 'Reel today' : 'Reels today',
              style: TextStyle(
                fontSize: 16,
                fontWeight: FontWeight.w500,
                color: secondaryTextColor,
              ),
            ),

            const SizedBox(height: 24),

            // Limit Reached State Banner
            if (limitReached)
              Container(
                margin: const EdgeInsets.only(bottom: 20),
                padding: const EdgeInsets.symmetric(
                  horizontal: 16,
                  vertical: 12,
                ),
                decoration: BoxDecoration(
                  color: AppColors.coral.withValues(alpha: 0.12),
                  borderRadius: BorderRadius.circular(12),
                  border: Border.all(
                    color: AppColors.coral.withValues(alpha: 0.5),
                  ),
                ),
                child: Row(
                  children: [
                    const Icon(
                      Icons.block_rounded,
                      color: AppColors.coral,
                      size: 22,
                    ),
                    const SizedBox(width: 12),
                    Expanded(
                      child: Text(
                        'Your daily limit is reached. MindLol blocker is active.',
                        style: TextStyle(
                          fontSize: 13,
                          fontWeight: FontWeight.w600,
                          color: primaryTextColor,
                        ),
                      ),
                    ),
                  ],
                ),
              ),

            // Progress Visualization Card
            ProgressCard(
              todayCount: todayCount,
              dailyLimit: dailyLimit,
              blobState: blobState,
            ),

            const SizedBox(height: 20),

            // Daily Limit Control
            Row(
              mainAxisAlignment: MainAxisAlignment.spaceBetween,
              children: [
                Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text(
                      'Daily limit',
                      style: TextStyle(
                        fontSize: 13,
                        color: secondaryTextColor,
                      ),
                    ),
                    const SizedBox(height: 2),
                    Text(
                      '$dailyLimit Reels',
                      style: TextStyle(
                        fontSize: 16,
                        fontWeight: FontWeight.w600,
                        color: primaryTextColor,
                      ),
                    ),
                  ],
                ),
                OutlinedButton.icon(
                  onPressed: _openLimitEditModal,
                  icon: const Icon(Icons.edit_outlined, size: 16),
                  label: const Text('Edit'),
                  style: OutlinedButton.styleFrom(
                    padding: const EdgeInsets.symmetric(
                      horizontal: 16,
                      vertical: 10,
                    ),
                  ),
                ),
              ],
            ),

            const SizedBox(height: 32),
          ],
        ),
      ),
    );
  }
}

