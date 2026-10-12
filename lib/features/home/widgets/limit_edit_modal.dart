import 'package:flutter/material.dart';
import '../../../app/theme/app_colors.dart';

/// Bottom sheet modal for adjusting daily short-form video limit.
///
/// Features:
/// - Quick preset chips (10, 15, 20, 30, 50, 100)
/// - Custom limit input with validation
/// - Minimum 48dp touch targets
/// - Clean sentence-case copy
class LimitEditModal extends StatefulWidget {
  final int currentLimit;
  final Function(int newLimit) onSave;

  const LimitEditModal({
    super.key,
    required this.currentLimit,
    required this.onSave,
  });

  static Future<void> show(
    BuildContext context, {
    required int currentLimit,
    required Function(int newLimit) onSave,
  }) {
    return showModalBottomSheet(
      context: context,
      isScrollControlled: true,
      backgroundColor: Colors.transparent,
      builder: (context) => LimitEditModal(
        currentLimit: currentLimit,
        onSave: onSave,
      ),
    );
  }

  @override
  State<LimitEditModal> createState() => _LimitEditModalState();
}

class _LimitEditModalState extends State<LimitEditModal> {
  late int _selectedLimit;
  final TextEditingController _customController = TextEditingController();
  final List<int> _quickPresets = [10, 15, 20, 30, 50, 100];
  bool _isCustom = false;

  @override
  void initState() {
    super.initState();
    _selectedLimit = widget.currentLimit;
    if (!_quickPresets.contains(_selectedLimit)) {
      _isCustom = true;
      _customController.text = _selectedLimit.toString();
    }
  }

  @override
  void dispose() {
    _customController.dispose();
    super.dispose();
  }

  void _save() {
    int finalLimit = _selectedLimit;
    if (_isCustom) {
      final parsed = int.tryParse(_customController.text);
      if (parsed != null && parsed > 0) {
        finalLimit = parsed;
      }
    }
    widget.onSave(finalLimit);
    Navigator.of(context).pop();
  }

  @override
  Widget build(BuildContext context) {
    final isDark = Theme.of(context).brightness == Brightness.dark;
    final surfaceColor =
        isDark ? AppColors.darkSurface : AppColors.lightSurface;
    final textColor =
        isDark ? AppColors.darkTextPrimary : AppColors.lightTextPrimary;
    final secondaryTextColor =
        isDark ? AppColors.darkTextSecondary : AppColors.lightTextSecondary;

    return Padding(
      padding: EdgeInsets.only(
        bottom: MediaQuery.of(context).viewInsets.bottom,
      ),
      child: Container(
        padding: const EdgeInsets.symmetric(horizontal: 24, vertical: 20),
        decoration: BoxDecoration(
          color: surfaceColor,
          borderRadius: const BorderRadius.vertical(top: Radius.circular(24)),
          border: Border.all(
            color: isDark ? AppColors.darkBorder : AppColors.lightBorder,
            width: 1,
          ),
        ),
        child: Column(
          mainAxisSize: MainAxisSize.min,
          crossAxisAlignment: CrossAxisAlignment.stretch,
          children: [
            Center(
              child: Container(
                width: 36,
                height: 4,
                decoration: BoxDecoration(
                  color: secondaryTextColor.withValues(alpha: 0.3),
                  borderRadius: BorderRadius.circular(2),
                ),
              ),
            ),
            const SizedBox(height: 18),
            Text(
              'Set daily limit',
              style: TextStyle(
                fontSize: 20,
                fontWeight: FontWeight.w700,
                letterSpacing: -0.2,
                color: textColor,
              ),
              textAlign: TextAlign.center,
            ),
            const SizedBox(height: 6),
            Text(
              'MindLol will overlay a gentle blocker once you hit this limit.',
              style: TextStyle(
                fontSize: 14,
                height: 1.35,
                color: secondaryTextColor,
              ),
              textAlign: TextAlign.center,
            ),
            const SizedBox(height: 22),
            Wrap(
              spacing: 10,
              runSpacing: 10,
              alignment: WrapAlignment.center,
              children: [
                ..._quickPresets.map((preset) {
                  final isSelected = !_isCustom && _selectedLimit == preset;
                  return ChoiceChip(
                    label: Text('$preset videos'),
                    selected: isSelected,
                    selectedColor: AppColors.primaryTeal,
                    backgroundColor: isDark
                        ? AppColors.darkSurfaceContainer
                        : AppColors.lightSurfaceContainer,
                    labelStyle: TextStyle(
                      color: isSelected
                          ? Colors.white
                          : (isDark
                              ? AppColors.darkTextPrimary
                              : AppColors.lightTextPrimary),
                      fontWeight: FontWeight.w600,
                    ),
                    onSelected: (selected) {
                      if (selected) {
                        setState(() {
                          _selectedLimit = preset;
                          _isCustom = false;
                        });
                      }
                    },
                  );
                }),
              ],
            ),
            const SizedBox(height: 20),
            Row(
              mainAxisAlignment: MainAxisAlignment.center,
              children: [
                Text(
                  'Selected limit: ',
                  style: TextStyle(color: secondaryTextColor, fontSize: 14),
                ),
                Text(
                  '${_isCustom ? (_customController.text.isEmpty ? '?' : _customController.text) : _selectedLimit} videos',
                  style: const TextStyle(
                    color: AppColors.primaryTeal,
                    fontSize: 15,
                    fontWeight: FontWeight.w700,
                  ),
                ),
              ],
            ),
            const SizedBox(height: 22),
            ElevatedButton(
              onPressed: _save,
              child: const Text('Save limit'),
            ),
            const SizedBox(height: 8),
          ],
        ),
      ),
    );
  }
}
