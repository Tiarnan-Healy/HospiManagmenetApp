# Lab 2

### Overview

Lab 2 builds on the Lab 1 foundation to add appointment management, biometric authentication,
and secure API communication via Retrofit. It introduces a modular feature package structure,
a repository pattern, domain use cases, and RBAC-gated access to the appointments module.

---

### What This Lab Demonstrates

- Appointment scheduling with conflict detection
- Biometric authentication with PIN fallback via `BiometricLoginCoordinator`
- Retrofit integration with a `MockInterceptor` simulating a real API
- Network resilience via timeout configuration and a `RetryInterceptor`
- Expanded RBAC via `RbacPolicyEvaluator` covering ADMIN, RECEPTION, and CLINICIAN roles
- Repository pattern separating network and local data concerns
- Domain use cases encapsulating business logic independently of the UI layer

---

### Package Structure

- `data/entities/Appointment.java` - Room entity for appointment records
- `data/dao/AppointmentDao.java` - queries including overlap detection and date range filtering
- `data/repo/AppointmentRepository.java` - coordinates network fetch and Room caching
- `domain/` - `GetTodaysAppointmentsUseCase`, `BookOrRescheduleAppointmentUseCase`, 
`DetectScheduleConflictsUseCase`
- `network/` - `AppointmentApi`, `ApiClient`, `MockInterceptor`, `RetryInterceptor`
- `network/dto/` - `AppointmentDto`, `ClinicDto`, `DoctorDto`
- `security/auth/` - `BiometricLoginCoordinator`, `RbacPolicyEvaluator`
- `feature/appointments/ui/` - `AppointmentActivity`, `AppointmentListFragment`, `BookingFragment`
- `feature/appointments/ui/adapters/` - `AppointmentAdapter`
- `assets/mock/` - `appointments_today.json`, `booking_success.json`

---

### Features

- **Appointment List** - loads today's appointments from the mock API, caches to Room, displays in a filterable RecyclerView
- **Clinic Filter** - Spinner filters the displayed list by clinic location via a parameterised Room query
- **Booking and Rescheduling** - pre-fills a booking form from a selected appointment; submits via the repository
- **Conflict Detection** - checks the Room database for overlapping appointments for the same clinician before confirming a booking
- **Biometric Gate** - `AppointmentActivity` requires biometric authentication on entry; falls back to PIN if biometrics are unavailable
- **RBAC** - viewing appointments requires ADMIN, RECEPTION, or CLINICIAN role; booking requires ADMIN or RECEPTION

---

### Security

- Biometric authentication enforced before any appointment data is shown
- PIN fallback prevents lockout when biometrics are unavailable
- RBAC checks applied at both the Activity and Fragment level
- All Room operations run on background threads via `Executors`
- `RetryInterceptor` handles transient network failures without exposing error detail
- PHI is never written to logs - error handlers log event counts only

---

### Known Limitations

- **Booking form uses raw epoch milliseconds** - date and time pickers will replace this in Lab 3
- **Mock API always returns the full appointment list** - clinic filtering is applied at the Room query level rather than the network level
- **Timestamps in mock JSON must fall within today's date range** - static JSON does not update automatically