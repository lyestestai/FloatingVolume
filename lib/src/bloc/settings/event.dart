sealed class Event {
  const Event();
}

class Initialize extends Event {
  const Initialize();
}

class ToggleAutoStart extends Event {
  const ToggleAutoStart();
}

class ToggleRestoreState extends Event {
  const ToggleRestoreState();
}

class SetAutoStartDelay extends Event {
  final int delayMs;

  const SetAutoStartDelay(this.delayMs);
}
