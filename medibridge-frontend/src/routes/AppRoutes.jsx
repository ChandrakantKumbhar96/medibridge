import { Routes, Route, Navigate } from 'react-router-dom'
import ProtectedRoute from '../components/routing/ProtectedRoute'

// Public
import LandingPage from '../pages/public/LandingPage'
import LoginPage from '../pages/public/LoginPage'
import AdminLoginPage from '../pages/public/AdminLoginPage'

// Patient
import PatientOverview from '../pages/patient/PatientOverview'
import PatientAppointments from '../pages/patient/PatientAppointments'
import FindDoctors from '../pages/patient/FindDoctors'
import BookAppointment from '../pages/patient/BookAppointment'
import MedicalRecords from '../pages/patient/MedicalRecords'
import PatientSettings from '../pages/patient/PatientSettings'
import PaymentPage from '../pages/patient/PaymentPage'
import RateExperience from '../pages/patient/RateExperience'

// Doctor
import DoctorOverview from '../pages/doctor/DoctorOverview'
import DoctorAppointments from '../pages/doctor/DoctorAppointments'
import PatientRecordsDoctor from '../pages/doctor/PatientRecordsDoctor'
import ManageSchedule from '../pages/doctor/ManageSchedule'
import DoctorSettings from '../pages/doctor/DoctorSettings'
import WritePrescription from '../pages/doctor/WritePrescription'
import DoctorEarnings from '../pages/doctor/DoctorEarnings'

// Admin
import AdminOverview from '../pages/admin/AdminOverview'
import ManagePatients from '../pages/admin/ManagePatients'
import ManageDoctors from '../pages/admin/ManageDoctors'
import AdminAppointments from '../pages/admin/AdminAppointments'
import Analytics from '../pages/admin/Analytics'
import Payouts from '../pages/admin/Payouts'
import SystemSettings from '../pages/admin/SystemSettings'

const patient = (el) => <ProtectedRoute role="patient">{el}</ProtectedRoute>
const doctor = (el) => <ProtectedRoute role="doctor">{el}</ProtectedRoute>
const admin = (el) => <ProtectedRoute role="admin">{el}</ProtectedRoute>

export default function AppRoutes() {
  return (
    <Routes>
      {/* Public */}
      <Route path="/" element={<LandingPage />} />
      <Route path="/login" element={<LoginPage />} />
      <Route path="/admin/login" element={<AdminLoginPage />} />

      {/* Patient */}
      <Route path="/patient" element={patient(<PatientOverview />)} />
      <Route path="/patient/appointments" element={patient(<PatientAppointments />)} />
      <Route path="/patient/find-doctors" element={patient(<FindDoctors />)} />
      <Route path="/patient/book" element={patient(<BookAppointment />)} />
      <Route path="/patient/payment" element={patient(<PaymentPage />)} />
      <Route path="/patient/rate" element={patient(<RateExperience />)} />
      <Route path="/patient/records" element={patient(<MedicalRecords />)} />
      <Route path="/patient/settings" element={patient(<PatientSettings />)} />

      {/* Doctor */}
      <Route path="/doctor" element={doctor(<DoctorOverview />)} />
      <Route path="/doctor/appointments" element={doctor(<DoctorAppointments />)} />
      <Route path="/doctor/patients" element={doctor(<PatientRecordsDoctor />)} />
      <Route path="/doctor/prescribe/:appointmentId" element={doctor(<WritePrescription />)} />
      <Route path="/doctor/earnings" element={doctor(<DoctorEarnings />)} />
      <Route path="/doctor/schedule" element={doctor(<ManageSchedule />)} />
      <Route path="/doctor/settings" element={doctor(<DoctorSettings />)} />

      {/* Admin */}
      <Route path="/admin" element={admin(<AdminOverview />)} />
      <Route path="/admin/patients" element={admin(<ManagePatients />)} />
      <Route path="/admin/doctors" element={admin(<ManageDoctors />)} />
      <Route path="/admin/appointments" element={admin(<AdminAppointments />)} />
      <Route path="/admin/payouts" element={admin(<Payouts />)} />
      <Route path="/admin/analytics" element={admin(<Analytics />)} />
      <Route path="/admin/settings" element={admin(<SystemSettings />)} />

      <Route path="*" element={<Navigate to="/" replace />} />
    </Routes>
  )
}
