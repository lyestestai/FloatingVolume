import 'package:floating_volume/src/ext.dart';
import 'package:floating_volume/src/single.dart';
import 'package:floating_volume/src/widgets/cool_switch.dart';
import 'package:flutter/material.dart';
import 'package:flutter_bloc/flutter_bloc.dart';
import 'package:flutter_svg/flutter_svg.dart';
import 'package:gap/gap.dart';
import 'package:url_launcher/url_launcher_string.dart';

import 'permissions.dart';

import 'package:floating_volume/src/bloc/status/bloc.dart' as bstatus;
import 'package:floating_volume/src/bloc/status/event.dart' as estatus;
import 'package:floating_volume/src/bloc/status/state.dart' as sstatus;

import 'package:floating_volume/src/bloc/permissions/bloc.dart' as bpermissions;
import 'package:floating_volume/src/bloc/permissions/state.dart'
    as spermissions;

import 'package:floating_volume/src/bloc/theme/bloc.dart' as btheme;
import 'package:floating_volume/src/bloc/theme/state.dart' as stheme;
import 'package:floating_volume/src/bloc/theme/event.dart' as etheme;

import 'package:floating_volume/src/bloc/settings.dart' as bsettings;
import 'package:floating_volume/src/bloc/settings.dart' as esettings;
import 'package:floating_volume/src/bloc/settings.dart' as ssettings;

import 'package:floating_volume/src/bloc/appearance/bloc.dart' as bappearance;
import 'package:floating_volume/src/bloc/appearance/event.dart' as eappearance;
import 'package:floating_volume/src/bloc/appearance/state.dart' as sappearance;

class HomeScreen extends StatefulWidget {
  const HomeScreen({super.key});

  @override
  State<HomeScreen> createState() => _HomeScreenState();
}

class _HomeScreenState extends State<HomeScreen> {
  int _currentIndex = 0;

  final List<Widget> _tabs = [
    const _AppearanceTab(),
    const _BehaviorTab(),
    const _AboutTab(),
  ];

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const Text("Floating Volume"),
        centerTitle: true,
        actions: [
          BlocBuilder<bstatus.Bloc, sstatus.State>(
            builder: (context, state) {
              return Padding(
                padding: const EdgeInsets.only(right: 16.0),
                child: CoolSwitch(
                  value: state.isEnabled,
                  width: 60,
                ),
              );
            },
          ),
          // Wrap the actual action inside a gesture detector to catch taps
          // because CoolSwitch absorbs its own taps internally
          Positioned.fill(
             child: GestureDetector(
                behavior: HitTestBehavior.translucent,
                onTap: () async {
                  final isGranted = context.read<bpermissions.Bloc>().state.isGranted;
                  if (!isGranted) {
                    await nativeApi.showToast("Please grant permissions first.");
                    if(context.mounted) {
                       context.push((_) => const PermissionsScreen());
                    }
                    return;
                  }
                  if(context.mounted) {
                     context.read<bstatus.Bloc>().add(estatus.Event.toggle);
                  }
                },
                child: Container(),
             ),
          )
        ],
      ),
      body: _tabs[_currentIndex],
      bottomNavigationBar: NavigationBar(
        selectedIndex: _currentIndex,
        onDestinationSelected: (index) {
          setState(() {
            _currentIndex = index;
          });
        },
        destinations: const [
          NavigationDestination(
            icon: Icon(Icons.palette_outlined),
            selectedIcon: Icon(Icons.palette),
            label: 'Appearance',
          ),
          NavigationDestination(
            icon: Icon(Icons.settings_outlined),
            selectedIcon: Icon(Icons.settings),
            label: 'Behavior',
          ),
          NavigationDestination(
            icon: Icon(Icons.info_outline),
            selectedIcon: Icon(Icons.info),
            label: 'About',
          ),
        ],
      ),
    );
  }
}

class _AppearanceTab extends StatelessWidget {
  const _AppearanceTab();

  @override
  Widget build(BuildContext context) {
    return ListView(
      padding: const EdgeInsets.all(16.0),
      children: [
        const Text("Widget Design", style: TextStyle(fontSize: 18, fontWeight: FontWeight.bold, color: Colors.blue)),
        const Gap(16),
        
        BlocBuilder<bappearance.AppearanceBloc, sappearance.AppearanceState>(
          builder: (context, state) {
            return Card(
              elevation: 4,
              shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(16)),
              child: Column(
                children: [
                  SwitchListTile(
                    title: const Text("Glassmorphism (Blur)"),
                    subtitle: const Text("Applies a blur effect behind the widget (Android 12+)"),
                    value: state.enableBlur,
                    onChanged: (val) {
                      context.read<bappearance.AppearanceBloc>().add(eappearance.SetBlur(val));
                    },
                  ),
                  const Divider(height: 1),
                  SwitchListTile(
                    title: const Text("Material You Colors"),
                    subtitle: const Text("Use system wallpaper colors for the slider (Android 12+)"),
                    value: state.enableDynamicColors,
                    onChanged: (val) {
                      context.read<bappearance.AppearanceBloc>().add(eappearance.SetDynamicColors(val));
                    },
                  ),
                  const Divider(height: 1),
                  Padding(
                    padding: const EdgeInsets.all(16.0),
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        const Text("Corner Radius"),
                        Slider(
                          value: state.cornerRadius,
                          min: 0,
                          max: 100,
                          divisions: 20,
                          label: "${state.cornerRadius.round()}",
                          onChanged: (val) {
                            context.read<bappearance.AppearanceBloc>().add(eappearance.SetCornerRadius(val));
                          },
                        ),
                      ],
                    ),
                  ),
                ],
              ),
            );
          },
        ),

        const Gap(32),
        const Text("App Theme", style: TextStyle(fontSize: 18, fontWeight: FontWeight.bold, color: Colors.blue)),
        const Gap(16),

        BlocBuilder<btheme.Bloc, stheme.State>(
          builder: (context, state) {
            return Card(
              elevation: 4,
              shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(16)),
              child: Padding(
                padding: const EdgeInsets.all(8.0),
                child: Row(
                  mainAxisAlignment: MainAxisAlignment.spaceEvenly,
                  children: stheme.Theme.values.map((t) {
                    final isSelected = state.theme == t;
                    return ChoiceChip(
                      label: Text(t.name.toUpperCase()),
                      selected: isSelected,
                      onSelected: (selected) {
                        if (selected) {
                          context.read<btheme.Bloc>().add(etheme.Change(t));
                        }
                      },
                    );
                  }).toList(),
                ),
              ),
            );
          },
        ),
      ],
    );
  }
}

class _BehaviorTab extends StatelessWidget {
  const _BehaviorTab();

  @override
  Widget build(BuildContext context) {
    return ListView(
      padding: const EdgeInsets.all(16.0),
      children: [
        const Text("Service Control", style: TextStyle(fontSize: 18, fontWeight: FontWeight.bold, color: Colors.blue)),
        const Gap(16),

        BlocBuilder<bsettings.Bloc, ssettings.State>(
          builder: (context, state) {
            return Card(
              elevation: 4,
              shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(16)),
              child: Column(
                children: [
                  SwitchListTile(
                    title: const Text("Auto-start on boot"),
                    subtitle: const Text("Launch the widget when you turn on your phone"),
                    value: state.autoStartEnabled,
                    onChanged: (val) {
                      context.read<bsettings.Bloc>().add(const esettings.ToggleAutoStart());
                    },
                  ),
                  const Divider(height: 1),
                  SwitchListTile(
                    title: const Text("Restore service state"),
                    subtitle: const Text("Remember if the widget was active before a reboot"),
                    value: state.restoreServiceState,
                    onChanged: (val) {
                      context.read<bsettings.Bloc>().add(const esettings.ToggleRestoreState());
                    },
                  ),
                  const Divider(height: 1),
                  Padding(
                    padding: const EdgeInsets.all(16.0),
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        Text("Startup Delay: ${(state.autoStartDelayMs / 1000).round()}s"),
                        Slider(
                          value: (state.autoStartDelayMs / 1000).roundToDouble(),
                          min: 0,
                          max: 30,
                          divisions: 30,
                          label: "${(state.autoStartDelayMs / 1000).round()}s",
                          onChanged: (val) {
                            context.read<bsettings.Bloc>().add(esettings.SetAutoStartDelay((val * 1000).toInt()));
                          },
                        ),
                      ],
                    ),
                  ),
                ],
              ),
            );
          },
        ),

        const Gap(32),
        const Text("Permissions", style: TextStyle(fontSize: 18, fontWeight: FontWeight.bold, color: Colors.blue)),
        const Gap(16),

        BlocBuilder<bpermissions.Bloc, spermissions.State>(
          builder: (context, state) {
            final Widget trailing;
            final String message;
            
            if (state.isInitialized) {
              if (state.isGranted) {
                trailing = const Icon(Icons.check_circle, color: Colors.green);
                message = "All required permissions are granted.";
              } else {
                trailing = const Icon(Icons.error, color: Colors.red);
                message = "Some permissions are missing. Tap to fix.";
              }
            } else {
              trailing = const CircularProgressIndicator();
              message = "Loading...";
            }

            return Card(
              elevation: 4,
              shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(16)),
              child: ListTile(
                leading: const Icon(Icons.security),
                title: const Text("Manage Permissions"),
                subtitle: Text(message),
                trailing: trailing,
                onTap: () {
                  context.push((_) => const PermissionsScreen());
                },
              ),
            );
          },
        ),
      ],
    );
  }
}

class _AboutTab extends StatelessWidget {
  const _AboutTab();

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
              const Text("Version 0.8.0", style: TextStyle(color: Colors.grey)),
            ],
          ),
        ),
        const Gap(32),

        const Text("Tools & Debugging", style: TextStyle(fontSize: 18, fontWeight: FontWeight.bold, color: Colors.blue)),
        const Gap(16),

        Card(
          elevation: 4,
          shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(16)),
          child: Column(
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
        ),

        const Gap(32),
        const Text("Links", style: TextStyle(fontSize: 18, fontWeight: FontWeight.bold, color: Colors.blue)),
        const Gap(16),

        Card(
          elevation: 4,
          shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(16)),
          child: Column(
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
          ),
        )
      ],
    );
  }
}
