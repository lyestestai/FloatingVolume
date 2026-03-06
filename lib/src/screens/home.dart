import 'package:floating_volume/src/ext.dart';
import 'package:floating_volume/src/single.dart';
import 'package:floating_volume/src/widgets/cool_switch.dart';
import 'package:flutter/material.dart';
import 'package:flutter_bloc/flutter_bloc.dart';

import 'package:floating_volume/src/screens/tabs/appearance.dart';
import 'package:floating_volume/src/screens/tabs/behavior.dart';
import 'package:floating_volume/src/screens/tabs/about.dart';

import 'permissions.dart';

import 'package:floating_volume/src/bloc/status/bloc.dart' as bstatus;
import 'package:floating_volume/src/bloc/status/event.dart' as estatus;
import 'package:floating_volume/src/bloc/status/state.dart' as sstatus;

import 'package:floating_volume/src/bloc/permissions/bloc.dart' as bpermissions;

class HomeScreen extends StatefulWidget {
  const HomeScreen({super.key});

  @override
  State<HomeScreen> createState() => _HomeScreenState();
}

class _HomeScreenState extends State<HomeScreen> {
  int _currentIndex = 0;

  final List<Widget> _tabs = [
    const AppearanceTab(),
    const BehaviorTab(),
    const AboutTab(),
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
                child: GestureDetector(
                  behavior: HitTestBehavior.translucent,
                  onTap: () async {
                    final isGranted = context.read<bpermissions.Bloc>().state.isGranted;
                    if (!isGranted) {
                      await nativeApi.showToast("Please grant permissions first.");
                      if (context.mounted) {
                        context.push((_) => const PermissionsScreen());
                      }
                      return;
                    }
                    if (context.mounted) {
                      context.read<bstatus.Bloc>().add(const estatus.Toggle());
                    }
                  },
                  child: CoolSwitch(
                    value: state.isEnabled,
                    width: 60,
                  ),
                ),
              );
            },
          ),
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
