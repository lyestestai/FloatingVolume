abstract class Event { const Event(); }
class Initialize extends Event { const Initialize(); }
class Enable extends Event { const Enable(); }
class Disable extends Event { const Disable(); }
class Toggle extends Event { const Toggle(); }
class ServiceStatusChanged extends Event {
  final bool isEnabled;
  const ServiceStatusChanged(this.isEnabled);
}
class VisibilityChanged extends Event {
  final bool isVisible;
  const VisibilityChanged(this.isVisible);
}
