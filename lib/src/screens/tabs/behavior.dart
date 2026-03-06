import 'package:floating_volume/src/ext.dart';
import 'package:floating_volume/src/widgets/settings.dart';
import 'package:flutter/material.dart';
import 'package:flutter_bloc/flutter_bloc.dart';
import 'package:gap/gap.dart';

import '../permissions.dart';

import 'package:floating_volume/src/bloc/permissions/bloc.dart' as bpermissions;
import 'package:floating_volume/src/bloc/permissions/state.dart' as spermissions;

import 'package:floating_volume/src/bloc/settings.dart' as bsettings;
import 'package:floating_volume/src/bloc/settings.dart' as esettings;
import 'package:floating_volume/src/bloc/settings.dart' as ssettings;

class BehaviorTab extends StatelessWidget {
  const BehaviorTab({super.key});

  @override
  Widget build(BuildContext context) {
    return ListView(
      padding: const EdgeInsets.all(16.0),
      children: [
        const SectionTitle("Service Control"),
        const Gap(16),

        SettingsCard(
          children: [
            BlocBuilder<bsettings.Bloc, ssettings.State>(
              buildWhen: (p, c) => p.autoStartEnabled != c.autoStartEnabled,
              builder: (context, state) => SwitchListTile(
                title: const Text("Auto-start on boot"),
                subtitle: const Text("Launch the widget when you turn on your phone"),
                value: state.autoStartEnabled,
                onChanged: (val) => context.read<bsettings.Bloc>().add(const esettings.ToggleAutoStart()),
              ),
            ),
            const Divider(height: 1),
            BlocBuilder<bsettings.Bloc, ssettings.State>(
              buildWhen: (p, c) => p.restoreServiceState != c.restoreServiceState,
              builder: (context, state) => SwitchListTile(
                title: const Text("Restore service state"),
                subtitle: const Text("Remember if the widget was active before a reboot"),
                value: state.restoreServiceState,
                onChanged: (val) => context.read<bsettings.Bloc>().add(const esettings.ToggleRestoreState()),
              ),
            ),
            const Divider(height: 1),
            Padding(
              padding: const EdgeInsets.all(16.0),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  BlocBuilder<bsettings.Bloc, ssettings.State>(
                    buildWhen: (p, c) => p.autoStartDelayMs != c.autoStartDelayMs,
                    builder: (context, state) => Text("Startup Delay: ${(state.autoStartDelayMs / 1000).round()}s"),
                  ),
                  BlocBuilder<bsettings.Bloc, ssettings.State>(
                    buildWhen: (p, c) => p.autoStartDelayMs != c.autoStartDelayMs,
                    builder: (context, state) => Slider(
                      value: (state.autoStartDelayMs / 1000).roundToDouble(),
                      min: 0,
                      max: 30,
                      divisions: 30,
                      label: "${(state.autoStartDelayMs / 1000).round()}s",
                      onChanged: (val) => context.read<bsettings.Bloc>().add(esettings.SetAutoStartDelay((val * 1000).toInt())),
                    ),
                  ),
                ],
              ),
            ),
          ],
        ),

        const Gap(32),
        const SectionTitle("Permissions"),
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

            return SettingsCard(
              children: [
                ListTile(
                  leading: const Icon(Icons.security),
                  title: const Text("Manage Permissions"),
                  subtitle: Text(message),
                  trailing: trailing,
                  onTap: () {
                    context.push((_) => const PermissionsScreen());
                  },
                ),
              ],
            );
          },
        ),
      ],
    );
  }
}
