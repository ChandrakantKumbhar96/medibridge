// Realistic mock data mirroring the MySQL schema. Used when VITE_USE_MOCK=true
// so the whole app runs with no backend. Replace by flipping VITE_USE_MOCK=false.

export const specializations = [
  'Cardiology', 'Dermatology', 'General Physician',
  'Orthopedics', 'Pediatrics', 'Neurology',
]

export const specialtyCards = [
  { name: 'Cardiology', emoji: '❤️', doctors: 12 },
  { name: 'Dermatology', emoji: '🔬', doctors: 8 },
  { name: 'General Physician', emoji: '🩺', doctors: 15 },
  { name: 'Orthopedics', emoji: '🦴', doctors: 10 },
  { name: 'Pediatrics', emoji: '👶', doctors: 9 },
  { name: 'Neurology', emoji: '🧠', doctors: 6 },
]

export const doctors = [
  { doctor_id: 'd-1', full_name: 'Dr. Sarah Johnson', specialization: 'Cardiologist', rating: 4.8, experience_years: 15, status: 'active', available: true, consultation_fee: 150, consultation_duration_min: 60, email: 'sarah.j@medibridge.com', phone: '+1 234 567 8900', license_number: 'MD-12345-2020', patients: 156, bio: 'Experienced cardiologist with 15 years of practice. Specialized in preventive cardiology and heart disease management.' },
  { doctor_id: 'd-2', full_name: 'Dr. Michael Chen', specialization: 'Dermatologist', rating: 4.9, experience_years: 12, status: 'active', available: true, consultation_fee: 120, consultation_duration_min: 30, email: 'michael.c@medibridge.com', phone: '+1 234 567 8901', license_number: 'MD-12346-2020', patients: 98, bio: 'Board-certified dermatologist focused on skin health and cosmetic dermatology.' },
  { doctor_id: 'd-3', full_name: 'Dr. Emily Brown', specialization: 'General Physician', rating: 4.7, experience_years: 10, status: 'active', available: false, consultation_fee: 90, consultation_duration_min: 30, email: 'emily.b@medibridge.com', phone: '+1 234 567 8902', license_number: 'MD-12347-2020', patients: 134, bio: 'General physician providing comprehensive primary care for all ages.' },
  { doctor_id: 'd-4', full_name: 'Dr. Robert Wilson', specialization: 'Orthopedic', rating: 4.6, experience_years: 18, status: 'suspended', available: true, consultation_fee: 180, consultation_duration_min: 45, email: 'robert.w@medibridge.com', phone: '+1 234 567 8903', license_number: 'MD-12348-2020', patients: 87, bio: 'Orthopedic surgeon specializing in joint replacement and sports injuries.' },
  { doctor_id: 'd-5', full_name: 'Dr. Lisa Anderson', specialization: 'Pediatrician', rating: 4.9, experience_years: 14, status: 'active', available: true, consultation_fee: 110, consultation_duration_min: 30, email: 'lisa.a@medibridge.com', phone: '+1 234 567 8904', license_number: 'MD-12349-2020', patients: 142, bio: 'Compassionate pediatrician dedicated to children’s health and development.' },
]

export const patientAppointments = {
  upcoming: [
    { appointment_id: 'a-1', doctor: 'Dr. Sarah Johnson', specialization: 'Cardiologist', appointment_date: '2026-05-05', time: '10:00 AM', status: 'confirmed' },
    { appointment_id: 'a-2', doctor: 'Dr. Michael Chen', specialization: 'Dermatologist', appointment_date: '2026-05-08', time: '02:30 PM', status: 'pending' },
    { appointment_id: 'a-3', doctor: 'Dr. Emily Brown', specialization: 'General Physician', appointment_date: '2026-05-12', time: '11:00 AM', status: 'confirmed' },
  ],
  past: [
    { appointment_id: 'a-4', doctor: 'Dr. Robert Wilson', specialization: 'Orthopedic', appointment_date: '2026-04-20', time: '03:00 PM', reason: 'Routine checkup' },
    { appointment_id: 'a-5', doctor: 'Dr. Sarah Johnson', specialization: 'Cardiologist', appointment_date: '2026-04-10', time: '10:00 AM', reason: 'Blood pressure monitoring' },
  ],
}

export const medicalRecords = [
  { report_id: 'r-1', report_name: 'Blood Test Report', report_type: 'Lab Report', upload_date: '2026-04-20', size: '2.4 MB' },
  { report_id: 'r-2', report_name: 'X-Ray Chest', report_type: 'Imaging', upload_date: '2026-03-15', size: '5.1 MB' },
  { report_id: 'r-3', report_name: 'Prescription - Dr. Johnson', report_type: 'Prescription', upload_date: '2026-04-10', size: '0.8 MB' },
  { report_id: 'r-4', report_name: 'Medical History', report_type: 'Document', upload_date: '2026-01-05', size: '1.2 MB' },
]

export const patientProfile = {
  patient_id: 101,
  full_name: 'John Doe',
  email: 'john.doe@email.com',
  phone: '+1 234 567 8900',
  date_of_birth: '1990-01-15',
  gender: 'Male',
  blood_group: 'O+',
}

// ---- Doctor portal data ----
export const doctorTodaySchedule = [
  { id: 't-1', time: '10:00 AM', name: 'John Doe', age: 35, type: 'Consultation', status: 'confirmed' },
  { id: 't-2', time: '11:30 AM', name: 'Sarah Miller', age: 28, type: 'Follow-up', status: 'confirmed' },
  { id: 't-3', time: '02:00 PM', name: 'Robert Brown', age: 52, type: 'New Patient', status: 'pending' },
  { id: 't-4', time: '03:30 PM', name: 'Emily Davis', age: 41, type: 'Consultation', status: 'pending' },
]

export const doctorPendingRequests = [
  { id: 'p-1', name: 'Michael Wilson', age: 45, date: '2026-05-06', time: '10:00 AM', reason: 'Chest pain consultation' },
  { id: 'p-2', name: 'Lisa Anderson', age: 33, date: '2026-05-07', time: '02:30 PM', reason: 'Regular checkup' },
  { id: 'p-3', name: 'David Martinez', age: 58, date: '2026-05-08', time: '11:00 AM', reason: 'Blood pressure monitoring' },
]

export const doctorCompletedConsults = [
  { id: 'c-1', name: 'James Taylor', age: 39, time: '09:00 AM', diagnosis: 'Viral infection', prescription: true },
  { id: 'c-2', name: 'Patricia Lee', age: 47, time: '08:00 AM', diagnosis: 'Routine checkup', prescription: false },
]

export const doctorPatientRecords = [
  { id: 'pr-1', name: 'John Doe', age: 35, last_visit: '2026-04-20', condition: 'Hypertension', next: '2026-05-05' },
  { id: 'pr-2', name: 'Sarah Miller', age: 28, last_visit: '2026-04-15', condition: 'Skin allergy', next: '2026-05-05' },
  { id: 'pr-3', name: 'Robert Brown', age: 52, last_visit: 'New Patient', condition: 'N/A', next: '2026-05-05' },
]

export const doctorSchedule = [
  { day: 'Monday', available: true, morning: true, afternoon: true },
  { day: 'Tuesday', available: true, morning: true, afternoon: false },
  { day: 'Wednesday', available: true, morning: true, afternoon: true },
  { day: 'Thursday', available: true, morning: true, afternoon: true },
  { day: 'Friday', available: true, morning: true, afternoon: false },
]

export const doctorProfile = {
  full_name: 'Dr. Sarah Johnson',
  specialization: 'Cardiology',
  license_number: 'MD-12345-2020',
  experience_years: 15,
  email: 'sarah.johnson@medibridge.com',
  phone: '+1 234 567 8900',
  bio: 'Experienced cardiologist with 15 years of practice. Specialized in preventive cardiology and heart disease management.',
  consultation_fee: 150,
  consultation_duration_min: 60,
}

// ---- Admin portal data ----
export const adminStats = {
  totalPatients: 1245,
  activeDoctors: 48,
  totalAppointments: 3567,
  activeToday: 124,
  completedToday: 45,
  revenueMTD: 125340,
}

export const adminRecentActivity = [
  { id: 'ac-1', name: 'Emily Davis', text: 'New patient registered', time: '2 hours ago', type: 'patient' },
  { id: 'ac-2', name: 'Dr. Sarah Johnson', text: 'Completed consultation with John Doe', time: '3 hours ago', type: 'consult' },
  { id: 'ac-3', name: 'Dr. Robert Wilson', text: 'Doctor account approved', time: '5 hours ago', type: 'doctor' },
  { id: 'ac-4', name: 'Sarah Miller', text: 'Cancelled appointment', time: '6 hours ago', type: 'cancel' },
]

export const adminPatients = [
  { patient_id: 1, full_name: 'John Doe', email: 'john.doe@email.com', phone: '+1 234 567 8900', join_date: '2026-01-15', appointments: 8, status: 'active' },
  { patient_id: 2, full_name: 'Sarah Miller', email: 'sarah.m@email.com', phone: '+1 234 567 8901', join_date: '2026-02-20', appointments: 5, status: 'active' },
  { patient_id: 3, full_name: 'Robert Brown', email: 'robert.b@email.com', phone: '+1 234 567 8902', join_date: '2026-03-10', appointments: 2, status: 'active' },
  { patient_id: 4, full_name: 'Emily Davis', email: 'emily.d@email.com', phone: '+1 234 567 8903', join_date: '2026-04-05', appointments: 0, status: 'inactive' },
]

export const adminDoctors = [
  { doctor_id: 'd-1', full_name: 'Dr. Sarah Johnson', email: 'sarah.j@medibridge.com', specialization: 'Cardiologist', license_number: 'MD-12345-2020', patients: 156, status: 'active' },
  { doctor_id: 'd-2', full_name: 'Dr. Michael Chen', email: 'michael.c@medibridge.com', specialization: 'Dermatologist', license_number: 'MD-12346-2020', patients: 98, status: 'active' },
  { doctor_id: 'd-3', full_name: 'Dr. Emily Brown', email: 'emily.b@medibridge.com', specialization: 'General Physician', license_number: 'MD-12347-2020', patients: 134, status: 'active' },
  { doctor_id: 'd-4', full_name: 'Dr. Robert Wilson', email: 'robert.w@medibridge.com', specialization: 'Orthopedic', license_number: 'MD-12348-2020', patients: 87, status: 'suspended' },
]

export const adminAppointments = [
  { appointment_id: 1, patient: 'John Doe', doctor: 'Dr. Sarah Johnson', date: '2026-05-05', time: '10:00 AM', type: 'Consultation', status: 'confirmed' },
  { appointment_id: 2, patient: 'Sarah Miller', doctor: 'Dr. Michael Chen', date: '2026-05-05', time: '11:30 AM', type: 'Follow-up', status: 'confirmed' },
  { appointment_id: 3, patient: 'Robert Brown', doctor: 'Dr. Emily Brown', date: '2026-05-06', time: '02:00 PM', type: 'New Patient', status: 'pending' },
  { appointment_id: 4, patient: 'Emily Davis', doctor: 'Dr. Robert Wilson', date: '2026-05-07', time: '03:30 PM', type: 'Consultation', status: 'cancelled' },
]

export const adminAnalytics = {
  monthly: { newPatients: 245, newDoctors: 8, totalAppointments: 1234, completionRate: '94.5%' },
  revenue: { consultations: 89450, followUps: 25890, newPatients: 10000, total: 125340 },
}

export const adminSystemSettings = {
  platformName: 'MediBridge',
  supportEmail: 'support@medibridge.com',
  maxAppointmentsPerDay: 50,
  twoFactor: true,
  sessionTimeout: '30 minutes',
}

export const timeSlots = ['09:00 AM', '09:30 AM', '10:00 AM', '10:30 AM', '11:00 AM', '11:30 AM', '02:00 PM', '02:30 PM', '03:00 PM', '03:30 PM', '04:00 PM', '04:30 PM']
