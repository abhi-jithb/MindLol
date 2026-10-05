import 'package:flutter/material.dart';
import '../../../app/theme/app_colors.dart';

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
        padding: const EdgeInsets.all(24),
        decoration: BoxDecoration(
          color: surfaceColor,
          borderRadius: const BorderRadius.vertical(top: Radius.circular(20)),
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
            const SizedBox(height: 20),
            Text(
              'Set Daily Reel Limit',
              style: TextStyle(
                fontSize: 20,
                fontWeight: FontWeight.bold,
                color: textColor,
              ),
              textAlign: TextAlign.center,
            ),
            const SizedBox(height: 8),
            Text(
              'MindLol will overlay a gentle blocker once you hit this limit.',
              style: TextStyle(
                fontSize: 14,
                color: secondaryTextColor,
              ),
              textAlign: TextAlign.center,
            ),
            const SizedBox(height: 24),
            Wrap(
              spacing: 10,
              runSpacing: 10,
              alignment: WrapAlignment.center,
              children: [
                ..._quickPresets.map((preset) {
                  final isSelected = !_isCustom && _selectedLimit == preset;
                  return ChoiceChip(
                    label: Text('$preset Reels'),
                    selected: isSelected,
                    selectedColor: AppColors.primaryTeal,
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
                  style: TextStyle(color: secondaryTextColor, fontSize: 15),
                ),
                Text(
                  '${_isCustom ? (_customController.text.isEmpty ? '?' : _customController.text) : _selectedLimit} Reels',
                  style: TextStyle(
                    color: AppColors.primaryTeal,
                    fontSize: 16,
                    fontWeight: FontWeight.bold,
                  ),
                ),
              ],
            ),
            const SizedBox(height: 24),
            ElevatedButton(
              onPressed: _save,
              child: const Text('Save Limit'),
            ),
            const SizedBox(height: 8),
          ],
        ),
      ),
    );
  }
}
