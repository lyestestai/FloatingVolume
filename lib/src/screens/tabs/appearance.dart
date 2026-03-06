import 'dart:ui';

import 'package:floating_volume/src/widgets/settings.dart';
import 'package:flutter/material.dart';
import 'package:flutter_bloc/flutter_bloc.dart';
import 'package:gap/gap.dart';

import 'package:floating_volume/src/bloc/appearance/bloc.dart' as bappearance;
import 'package:floating_volume/src/bloc/appearance/event.dart' as eappearance;
import 'package:floating_volume/src/bloc/appearance/state.dart' as sappearance;

import 'package:floating_volume/src/bloc/theme/bloc.dart' as btheme;
import 'package:floating_volume/src/bloc/theme/state.dart' as stheme;
import 'package:floating_volume/src/bloc/theme/event.dart' as etheme;

class AppearanceTab extends StatelessWidget {
  const AppearanceTab({super.key});

  @override
  Widget build(BuildContext context) {
    return ListView(
      padding: const EdgeInsets.all(16.0),
      children: [
        const SectionTitle("Design Preview"),
        const Gap(16),
        BlocBuilder<bappearance.AppearanceBloc, sappearance.AppearanceState>(
          builder: (context, state) {
            final color = state.enableDynamicColors 
               ? Theme.of(context).colorScheme.primary.withOpacity(state.enableBlur ? 0.5 : 0.8) 
               : Theme.of(context).primaryColorDark.withOpacity(state.enableBlur ? 0.3 : 0.6);
            
            return Center(
              child: Container(
                width: 60,
                height: 150,
                decoration: BoxDecoration(
                  color: color,
                  borderRadius: BorderRadius.circular(state.cornerRadius),
                  boxShadow: state.enableBlur ? [] : [const BoxShadow(color: Colors.black26, blurRadius: 10)],
                ),
                child: state.enableBlur 
                  ? ClipRRect(
                      borderRadius: BorderRadius.circular(state.cornerRadius),
                      child: BackdropFilter(
                        filter: ImageFilter.blur(sigmaX: 10, sigmaY: 10),
                        child: Container(color: Colors.transparent),
                      ),
                    ) 
                  : null,
              ),
            );
          }
        ),
        const Gap(32),

        const SectionTitle("Widget Design"),
        const Gap(16),
        
        SettingsCard(
          children: [
            BlocBuilder<bappearance.AppearanceBloc, sappearance.AppearanceState>(
              buildWhen: (p, c) => p.enableBlur != c.enableBlur,
              builder: (context, state) => SwitchListTile(
                title: const Text("Glassmorphism (Blur)"),
                subtitle: const Text("Applies a blur effect behind the widget (Android 12+)"),
                value: state.enableBlur,
                onChanged: (val) => context.read<bappearance.AppearanceBloc>().add(eappearance.SetBlur(val)),
              ),
            ),
            const Divider(height: 1),
            BlocBuilder<bappearance.AppearanceBloc, sappearance.AppearanceState>(
              buildWhen: (p, c) => p.enableDynamicColors != c.enableDynamicColors,
              builder: (context, state) => SwitchListTile(
                title: const Text("Material You Colors"),
                subtitle: const Text("Use system wallpaper colors for the slider (Android 12+)"),
                value: state.enableDynamicColors,
                onChanged: (val) => context.read<bappearance.AppearanceBloc>().add(eappearance.SetDynamicColors(val)),
              ),
            ),
            const Divider(height: 1),
            Padding(
              padding: const EdgeInsets.all(16.0),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  const Text("Corner Radius"),
                  BlocBuilder<bappearance.AppearanceBloc, sappearance.AppearanceState>(
                    buildWhen: (p, c) => p.cornerRadius != c.cornerRadius,
                    builder: (context, state) => Slider(
                      value: state.cornerRadius,
                      min: 0,
                      max: 100,
                      divisions: 20,
                      label: "${state.cornerRadius.round()}",
                      onChanged: (val) => context.read<bappearance.AppearanceBloc>().add(eappearance.SetCornerRadius(val)),
                    ),
                  ),
                ],
              ),
            ),
          ],
        ),

        const Gap(32),
        const SectionTitle("App Theme"),
        const Gap(16),

        BlocBuilder<btheme.Bloc, stheme.State>(
          builder: (context, state) {
            return SettingsCardPad(
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
            );
          },
        ),
      ],
    );
  }
}
