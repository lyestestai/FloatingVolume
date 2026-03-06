import 'package:floating_volume/src/ext.dart';
import 'package:floating_volume/src/single.dart';
import 'package:floating_volume/src/widgets/settings.dart';
import 'package:flutter/material.dart';
import 'package:flutter_svg/flutter_svg.dart';
import 'package:gap/gap.dart';
import 'package:url_launcher/url_launcher_string.dart';

class AboutTab extends StatelessWidget {
  const AboutTab({super.key});

  @override
  Widget build(BuildContext context) {
    return ListView(
      padding: const EdgeInsets.all(16.0),
      children: [
        Center(
          child: Column(
            children: [
              SvgPicture.asset("images/logo.svg", width: 80),
              const Gap(16),
              const Text("Floating Volume", style: TextStyle(fontSize: 24, fontWeight: FontWeight.bold)),
              const Text("Version 0.8.3", style: TextStyle(color: Colors.grey)),
            ],
          ),
        ),
        const Gap(32),

        const SectionTitle("Tools & Debugging"),
        const Gap(16),

        SettingsCard(
          children: [
            FutureBuilder(
              future: nativeApi.getLogStats(),
              builder: (context, snapshot) {
                if (!snapshot.hasData) {
                  return const ListTile(title: Text("Crash logs"), subtitle: Text("Loading..."));
                }
                final stats = snapshot.data!;
                final sizeKB = (stats.totalSizeBytes / 1024).toStringAsFixed(1);
                return ListTile(
                  leading: const Icon(Icons.bug_report),
                  title: const Text("Crash logs"),
                  subtitle: Text("${stats.totalFiles} files ($sizeKB KB)"),
                );
              },
            ),
            const Divider(height: 1),
            ListTile(
              leading: const Icon(Icons.share),
              title: const Text("Export logs"),
              onTap: () async {
                try {
                  await nativeApi.exportLogsAndShare();
                } catch (e) {
                  await nativeApi.showToast("No logs available");
                }
              },
            ),
            const Divider(height: 1),
            ListTile(
              leading: const Icon(Icons.delete, color: Colors.red),
              title: const Text("Clear logs", style: TextStyle(color: Colors.red)),
              onTap: () async {
                final confirmed = await showDialog<bool>(
                  context: context,
                  builder: (context) => AlertDialog(
                    title: const Text("Clear all logs?"),
                    actions: [
                      TextButton(onPressed: () => Navigator.pop(context, false), child: const Text("Cancel")),
                      TextButton(onPressed: () => Navigator.pop(context, true), child: const Text("Clear")),
                    ],
                  ),
                );
                if (confirmed == true) {
                  try {
                    await nativeApi.clearAllLogs();
                    if(context.mounted) {
                      ScaffoldMessenger.of(context).showSnackBar(const SnackBar(content: Text("Logs cleared")));
                    }
                  } catch (e) {}
                }
              },
            ),
          ],
        ),

        const Gap(32),
        const SectionTitle("Links"),
        const Gap(16),

        SettingsCard(
          children: [
            ListTile(
              leading: const Icon(Icons.code),
              title: const Text("Source Code"),
              trailing: const Icon(Icons.open_in_new),
              onTap: () => launchUrlString("https://github.com/mkalmousli/FloatingVolume", mode: LaunchMode.externalApplication),
            ),
            const Divider(height: 1),
            ListTile(
              leading: const Icon(Icons.coffee),
              title: const Text("Buy me a coffee"),
              trailing: const Icon(Icons.open_in_new),
              onTap: () => launchUrlString("https://www.ko-fi.com/mkalmousli", mode: LaunchMode.externalApplication),
            ),
            const Divider(height: 1),
            ListTile(
              leading: const Icon(Icons.article),
              title: const Text("License"),
              trailing: const Icon(Icons.open_in_new),
              onTap: () => launchUrlString("https://www.gnu.org/licenses/gpl-3.0.html", mode: LaunchMode.externalApplication),
            ),
          ],
        )
      ],
    );
  }
}
