import 'package:floating_volume/src/ext.dart';
import 'package:floating_volume/src/single.dart';
import 'package:floating_volume/src/widgets/cool_switch.dart';
import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:flutter_bloc/flutter_bloc.dart';
import 'package:flutter_svg/flutter_svg.dart';
import 'package:gap/gap.dart';
import 'package:url_launcher/url_launcher_string.dart';

import 'permissions.dart';

import 'package:floating_volume/src/bloc/status/bloc.dart' as bstatus;
import 'package:floating_volume/src/bloc/status/event.dart' as estatus;
import 'package:floating_volume/src/bloc/status/state.dart' as sstatus;

import 'package:floating_volume/src/bloc/permissions/bloc.dart' as bpermissions;
import 'package:floating_volume/src/bloc/permissions/event.dart'
    as epermissions;
import 'package:floating_volume/src/bloc/permissions/state.dart'
    as spermissions;

import 'package:floating_volume/src/bloc/theme/bloc.dart' as btheme;
import 'package:floating_volume/src/bloc/theme/state.dart' as stheme;
import 'package:floating_volume/src/bloc/theme/event.dart' as etheme;

class HomeScreen extends StatelessWidget {
  const HomeScreen({super.key});

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      body: SafeArea(
        child: ListView(
          children: [
            Gap(20),
            SvgPicture.asset(
              "images/logo.svg",
              width: 100,
              // colorFilter: ColorFilter.mode(Colors.grey, BlendMode.srcIn),
            ),
            Gap(20),
            Center(
              child: Text(
                "Floating Volume",
                style: Theme.of(context).textTheme.headlineMedium,
              ),
            ),
            Gap(50),

            BlocBuilder<bstatus.Bloc, sstatus.State>(
              builder: (context, state) {
                return GestureDetector(
                  child: CoolSwitch(value: state.isEnabled, width: 100),
                  onTap: () async {
                    final isGranted =
                        context.read<bpermissions.Bloc>().state.isGranted;

                    if (!isGranted) {
                      await nativeApi.showToast(
                        "Please grant permissions first.",
                      );

                      await context.push((_) => const PermissionsScreen());
                      return;
                    }

                    context.read<bstatus.Bloc>().add(estatus.Event.toggle);
                  },
                );
              },
            ),

            Gap(20),

            BlocBuilder<btheme.Bloc, stheme.State>(
              builder: (context, state) {
                return Padding(
                  padding: const EdgeInsets.all(8.0),
                  child: Row(
                    crossAxisAlignment: CrossAxisAlignment.center,
                    mainAxisAlignment: MainAxisAlignment.center,
                    children: [
                      ...stheme.Theme.values.map((t) {
                        final isSelected = state.theme == t;

                        return Padding(
                          padding: const EdgeInsets.all(8.0),
                          child: ElevatedButton.icon(
                            icon: isSelected ? Icon(Icons.check) : null,
                            onPressed:
                                state.theme == t
                                    ? null
                                    : () => context.read<btheme.Bloc>().add(
                                      etheme.Change(t),
                                    ),
                            label: Text(t.name),
                          ),
                        );
                      }),
                    ],
                  ),
                );
              },
            ),
            
            const AutoStartSettingsWidget(),
            const DiagnosticLogsWidget(),

            ListTile(
              title: const Text("Version"),
              trailing: const Text("v0.9.10"),
              subtitle: const Text("18 Oct 2026"),
            ),
            ListTile(
              title: const Text("Created by @mkalmousli"),
              subtitle: const Text("https://mkalmousli.dev"),
              trailing: SvgPicture.asset("images/mk.svg"),
              onTap: () async {
                await launchUrlString(
                  "https://mkalmousli.dev",
                  mode: LaunchMode.externalApplication,
                );
              },
            ),

            ListTile(
              title: const Text("Having an issue?"),
              subtitle: const Text("Report an issue on GitHub."),
              trailing: Icon(Icons.bug_report),
              onTap: () async {
                await launchUrlString(
                  "https://github.com/mkalmousli/FloatingVolume/issues",
                  mode: LaunchMode.externalApplication,
                );
              },
            ),
            ListTile(
              title: const Text("Like this app?"),
              subtitle: const Text(
                "If you want me to work on this more, you can buy me a coffee ;)",
              ),
              trailing: Icon(Icons.tag_faces),
              onTap: () async {
                await launchUrlString(
                  "https://www.ko-fi.com/mkalmousli",
                  mode: LaunchMode.externalApplication,
                );
              },
            ),

            BlocBuilder<bpermissions.Bloc, spermissions.State>(
              builder: (context, state) {
                final Widget trailing;
                if (state.isInitialized) {
                  if (state.isGranted) {
                    trailing = Icon(Icons.check_circle);
                  } else {
                    trailing = Icon(Icons.error, color: Colors.red);
                  }
                } else {
                  trailing = CircularProgressIndicator();
                }

                final String message;
                if (state.isInitialized) {
                  if (state.isGranted) {
                    message = "Permissions are granted.";
                  } else {
                    message = "Permissions are not granted.";
                  }
                } else {
                  message = "Retrieving permissions status...";
                }

                return ListTile(
                  title: const Text("Manage Permissions"),
                  subtitle: Text(message),
                  trailing: trailing,
                  onTap: () {
                    context.push((_) => const PermissionsScreen());
                  },
                );
              },
            ),
            ListTile(
              title: const Text("License"),
              subtitle: const Text(
                "This app is licensed under the GPL-3.0 License.",
              ),
              trailing: Icon(Icons.open_in_new),
              onTap: () async {
                await launchUrlString(
                  "https://www.gnu.org/licenses/gpl-3.0.html",
                  mode: LaunchMode.externalApplication,
                );
              },
            ),
            ListTile(
              title: const Text("Source Code"),
              subtitle: const Text("View the source code on GitHub."),
              trailing: Icon(Icons.open_in_new),
              onTap: () async {
                await launchUrlString(
                  "https://github.com/mkalmousli/FloatingVolume",
                  mode: LaunchMode.externalApplication,
                );
              },
            ),
            ListTile(
              title: const Text("Third-Party Libraries"),
              subtitle: const Text(
                "This app would not be possible without the help of these libraries.",
              ),
              trailing: Icon(Icons.info),
              onTap: () {
                showLicensePage(context: context);
              },
            ),
          ],
        ),
      ),
    );
  }
}

class AutoStartSettingsWidget extends StatefulWidget {
  const AutoStartSettingsWidget({super.key});

  @override
  State<AutoStartSettingsWidget> createState() => _AutoStartSettingsWidgetState();
}

class _AutoStartSettingsWidgetState extends State<AutoStartSettingsWidget> {
  bool _isLoading = true;
  bool _isEnabled = false;
  int _delayMs = 10000;

  @override
  void initState() {
    super.initState();
    _loadSettings();
  }

  Future<void> _loadSettings() async {
    try {
      final enabled = await nativeApi.getAutoStartEnabled();
      final delay = await nativeApi.getAutoStartDelay();
      int clampedDelay = delay;
      if (clampedDelay < 10000) clampedDelay = 10000;
      if (clampedDelay > 60000) clampedDelay = 60000;

      if (clampedDelay != delay) {
        await nativeApi.setAutoStartDelay(clampedDelay);
      }

      if (mounted) {
        setState(() {
          _isEnabled = enabled;
          _delayMs = clampedDelay;
          _isLoading = false;
        });
      }
    } catch (e) {
      if (mounted) {
        setState(() => _isLoading = false);
      }
    }
  }

  Future<void> _updateEnabled(bool val) async {
    setState(() => _isEnabled = val);
    await nativeApi.setAutoStartEnabled(val);
    if (val) {
      await nativeApi.setAutoStartDelay(_delayMs);
    }
  }

  Future<void> _updateDelay(double val) async {
    final newDelay = val.toInt().clamp(10000, 60000);
    setState(() => _delayMs = newDelay);
    await nativeApi.setAutoStartDelay(newDelay);
  }

  @override
  Widget build(BuildContext context) {
    if (_isLoading) {
      return const Padding(
        padding: EdgeInsets.all(16.0),
        child: Center(child: CircularProgressIndicator()),
      );
    }

    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        SwitchListTile(
          title: const Text("Démarrage Automatique (Auto-Start)"),
          subtitle: const Text("Lancer la bulle au démarrage de la voiture"),
          value: _isEnabled,
          onChanged: _updateEnabled,
          secondary: const Icon(Icons.rocket_launch),
        ),
        if (_isEnabled)
          Padding(
            padding: const EdgeInsets.symmetric(horizontal: 16.0, vertical: 8.0),
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(
                  "Délai avant lancement : ${(_delayMs / 1000).round()} secondes",
                  style: Theme.of(context).textTheme.bodyMedium,
                ),
                Slider(
                  value: _delayMs.toDouble(),
                  min: 10000,
                  max: 60000,
                  divisions: 50,
                  label: "${(_delayMs / 1000).round()} s",
                  onChanged: (val) {
                    setState(() => _delayMs = val.toInt());
                  },
                  onChangeEnd: _updateDelay,
                ),
                const Text(
                  "Un délai est recommandé pour laisser le système Android de la voiture démarrer ses services audio avant d'afficher la bulle.",
                  style: TextStyle(fontSize: 12, color: Colors.grey),
                ),
              ],
            ),
          ),
        const Divider(),
      ],
    );
  }
}

class DiagnosticLogsWidget extends StatefulWidget {
  const DiagnosticLogsWidget({super.key});

  @override
  State<DiagnosticLogsWidget> createState() => _DiagnosticLogsWidgetState();
}

class _DiagnosticLogsWidgetState extends State<DiagnosticLogsWidget> {
  bool _isLoading = true;
  bool _isEnabled = false;
  bool _isRefreshing = false;
  String _logsContent = "Chargement du journal de diagnostic...";

  @override
  void initState() {
    super.initState();
    _initLogsState();
  }

  Future<void> _initLogsState() async {
    try {
      final enabled = await nativeApi.getLogsEnabled();
      String content = "";
      if (enabled) {
        content = await nativeApi.getLogsContent();
      }
      if (mounted) {
        setState(() {
          _isEnabled = enabled;
          if (enabled) {
            _logsContent = content;
          }
          _isLoading = false;
        });
      }
    } catch (e) {
      if (mounted) {
        setState(() {
          _isLoading = false;
          _logsContent = "Erreur lors du chargement des logs : $e";
        });
      }
    }
  }

  Future<void> _toggleLogsEnabled(bool enabled) async {
    setState(() => _isEnabled = enabled);
    try {
      await nativeApi.setLogsEnabled(enabled);
      if (enabled) {
        await _refreshLogs();
      }
    } catch (e) {
      await nativeApi.showToast("Erreur: $e");
    }
  }

  Future<void> _refreshLogs() async {
    if (mounted) {
      setState(() => _isRefreshing = true);
    }
    try {
      final content = await nativeApi.getLogsContent();
      if (mounted) {
        setState(() {
          _logsContent = content;
          _isRefreshing = false;
        });
      }
    } catch (e) {
      if (mounted) {
        setState(() {
          _logsContent = "Impossible de lire les logs : $e";
          _isRefreshing = false;
        });
      }
    }
  }

  Future<void> _copyLogs() async {
    await _refreshLogs();
    await Clipboard.setData(ClipboardData(text: _logsContent));
    await nativeApi.showToast("Journal copié dans le presse-papiers !");
  }

  Future<void> _shareLogs() async {
    try {
      await nativeApi.exportLogsAndShare();
    } catch (e) {
      await nativeApi.showToast("Erreur lors du partage : $e");
    }
  }

  Future<void> _saveToDisk() async {
    try {
      final savedPath = await nativeApi.saveLogsToDisk();
      await _refreshLogs();
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(
            content: Text("Fichier enregistré : $savedPath"),
            duration: const Duration(seconds: 5),
          ),
        );
      }
      await nativeApi.showToast("Enregistré dans : $savedPath");
    } catch (e) {
      await nativeApi.showToast("Erreur d'enregistrement : $e");
    }
  }

  Future<void> _clearLogs() async {
    try {
      await nativeApi.clearAllLogs();
      await _refreshLogs();
      await nativeApi.showToast("Journal effacé.");
    } catch (e) {
      await nativeApi.showToast("Erreur lors de l'effacement : $e");
    }
  }

  @override
  Widget build(BuildContext context) {
    if (_isLoading) {
      return const SizedBox.shrink();
    }

    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        SwitchListTile(
          title: const Text("Journal de diagnostic (Logs)"),
          subtitle: const Text(
            "Afficher la fenêtre de logs (Auto-Start, réveil MCU, permissions)",
          ),
          value: _isEnabled,
          onChanged: _toggleLogsEnabled,
          secondary: const Icon(Icons.terminal),
        ),
        if (_isEnabled)
          Padding(
            padding: const EdgeInsets.symmetric(horizontal: 16.0, vertical: 8.0),
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.stretch,
              children: [
                Wrap(
                  spacing: 8.0,
                  runSpacing: 8.0,
                  children: [
                    ElevatedButton.icon(
                      onPressed: _copyLogs,
                      icon: const Icon(Icons.copy, size: 18),
                      label: const Text("Copier"),
                    ),
                    ElevatedButton.icon(
                      onPressed: _shareLogs,
                      icon: const Icon(Icons.share, size: 18),
                      label: const Text("Partager / Email"),
                    ),
                    ElevatedButton.icon(
                      onPressed: _saveToDisk,
                      icon: const Icon(Icons.save_alt, size: 18),
                      label: const Text("Enregistrer (Download/)"),
                    ),
                    OutlinedButton.icon(
                      onPressed: _isRefreshing ? null : _refreshLogs,
                      icon: const Icon(Icons.refresh, size: 18),
                      label: const Text("Actualiser"),
                    ),
                    OutlinedButton.icon(
                      onPressed: _clearLogs,
                      icon: const Icon(Icons.delete_outline, size: 18),
                      label: const Text("Effacer"),
                    ),
                  ],
                ),
                const Gap(10),
                Container(
                  height: 260,
                  padding: const EdgeInsets.all(10.0),
                  decoration: BoxDecoration(
                    color: const Color(0xFF121212),
                    borderRadius: BorderRadius.circular(8.0),
                    border: Border.all(color: Colors.grey.shade700),
                  ),
                  child: Scrollbar(
                    thumbVisibility: true,
                    child: SingleChildScrollView(
                      child: SelectableText(
                        _logsContent,
                        style: const TextStyle(
                          fontFamily: 'monospace',
                          fontSize: 11.5,
                          color: Color(0xFF80FF80),
                          height: 1.35,
                        ),
                      ),
                    ),
                  ),
                ),
                const Gap(6),
                const Text(
                  "Astuce : Cliquez sur 'Enregistrer (Download/)' pour sauvegarder un fichier .txt directement dans le dossier Téléchargements de l'autoradio, ou sur 'Partager / Email' pour l'envoyer.",
                  style: TextStyle(fontSize: 12, color: Colors.grey),
                ),
              ],
            ),
          ),
        const Divider(),
      ],
    );
  }
}

