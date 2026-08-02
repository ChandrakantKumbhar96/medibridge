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
  { doctor_id: 'd-1', full_name: 'Dr. Aditya Nair', specialization: 'Cardiologist', rating: 4.8, experience_years: 15, status: 'active', available: true, consultation_fee: 800, consultation_duration_min: 60, email: 'aditya.n@medibridge.com', phone: '+91 98765 43210', license_number: 'MD-12345-2020', patients: 156, bio: 'Experienced cardiologist with 15 years of practice. Specialized in preventive cardiology and heart disease management.' },
  { doctor_id: 'd-2', full_name: 'Dr. Rohan Mehta', specialization: 'Dermatologist', rating: 4.9, experience_years: 12, status: 'active', available: true, consultation_fee: 600, consultation_duration_min: 30, email: 'rohan.m@medibridge.com', phone: '+91 98765 43211', license_number: 'MD-12346-2020', patients: 98, bio: 'Board-certified dermatologist focused on skin health and cosmetic dermatology.' },
  { doctor_id: 'd-3', full_name: 'Dr. Meera Joshi', specialization: 'General Physician', rating: 4.7, experience_years: 10, status: 'active', available: false, consultation_fee: 500, consultation_duration_min: 30, email: 'meera.j@medibridge.com', phone: '+91 98765 43212', license_number: 'MD-12347-2020', patients: 134, bio: 'General physician providing comprehensive primary care for all ages.' },
  { doctor_id: 'd-4', full_name: 'Dr. Vikram Rao', specialization: 'Orthopedic', rating: 4.6, experience_years: 18, status: 'suspended', available: true, consultation_fee: 180, consultation_duration_min: 45, email: 'robert.w@medibridge.com', phone: '+91 98765 43213', license_number: 'MD-12348-2020', patients: 87, bio: 'Orthopedic surgeon specializing in joint replacement and sports injuries.' },
  { doctor_id: 'd-5', full_name: 'Dr. Anita Desai', specialization: 'Pediatrician', rating: 4.9, experience_years: 14, status: 'active', available: true, consultation_fee: 110, consultation_duration_min: 30, email: 'lisa.a@medibridge.com', phone: '+91 98765 43214', license_number: 'MD-12349-2020', patients: 142, bio: 'Compassionate pediatrician dedicated to children’s health and development.' },
]

// ISO local date-time N days out, matching what the server sends for
// follow_up_eligible_until. Sliced off the UTC string: the few hours of skew
// are meaningless against a window measured in days, and it keeps the fixture
// from needing a date library.
const isoInDays = (n) => new Date(Date.now() + n * 864e5).toISOString().slice(0, 19)

// Computed off "now" rather than a fixed string, so the "in N min" copy on the
// Next Available banner never goes stale the way a hardcoded fixture would.
const nextAvailableIn = (minutes) => {
  const at = new Date(Date.now() + minutes * 60e3)
  const availableDate = at.toISOString().slice(0, 10)
  const startTime = at.toLocaleTimeString('en-US', { hour: '2-digit', minute: '2-digit', hour12: true }).toUpperCase()
  return { availableDate, startTime }
}

export const nextAvailableSlot = (() => {
  const { availableDate, startTime } = nextAvailableIn(25)
  return {
    doctor_id: 'd-1',
    doctor_name: 'Dr. Aditya Nair',
    specialization: 'Cardiologist',
    schedule_id: 501,
    available_date: availableDate,
    start_time: startTime,
    fee: 800,
    wait_minutes: 25,
  }
})()

export const patientAppointments = {
  upcoming: [
    // Queue fields are sent only for today's confirmed bookings. Set here so
    // the live-queue line is visible in mock mode; delay_minutes is absent, not
    // 0, when the doctor is on time - the server omits it and QueueStatus reads
    // the absence.
    { appointment_id: 'a-1', doctor: 'Dr. Aditya Nair', specialization: 'Cardiologist', appointment_date: '2026-05-05', time: '10:00 AM', status: 'confirmed', queue_position: 3, eta_minutes: 50, delay_minutes: 25 },
    // Booked by the account holder for their daughter: `patient` is the child,
    // because that is who the doctor is seeing.
    { appointment_id: 'a-2', doctor: 'Dr. Rohan Mehta', specialization: 'Dermatologist', appointment_date: '2026-05-08', time: '02:30 PM', status: 'pending', patient: 'Ananya Gupta', age: 8, family_member_id: 201, booked_for: 'Child', account_holder: 'Aarav Gupta' },
    { appointment_id: 'a-3', doctor: 'Dr. Meera Joshi', specialization: 'General Physician', appointment_date: '2026-05-12', time: '11:00 AM', status: 'confirmed', free_cancellation: true, queue_position: 1, eta_minutes: 0 },
  ],
  past: [
    { appointment_id: 'a-4', doctor: 'Dr. Vikram Rao', specialization: 'Orthopedic', appointment_date: '2026-04-20', time: '03:00 PM', reason: 'Routine checkup' },
    // Completed, and still inside its free-follow-up window. The deadline is
    // relative rather than a literal date so the button does not quietly stop
    // appearing once these fixtures age past it. The server sends the field
    // only while the revisit is genuinely available (NON_NULL drops it
    // otherwise), so its presence alone is what the UI acts on.
    { appointment_id: 'a-5', doctor: 'Dr. Aditya Nair', doctor_id: 'd-1', specialization: 'Cardiologist', appointment_date: '2026-04-10', time: '10:00 AM', reason: 'Blood pressure monitoring', status: 'confirmed', follow_up_eligible_until: isoInDays(5) },
    { appointment_id: 'a-6', doctor: 'Dr. Rohan Mehta', specialization: 'Dermatologist', appointment_date: '2026-04-02', time: '09:30 AM', reason: 'Skin allergy', status: 'no_show', no_show_by: 'PATIENT' },
  ],
}

// Dependents on the account holder's login. They have no email and no password
// - they are people the account books for, not people who can sign in.
export const familyMembers = [
  { family_member_id: 201, full_name: 'Ananya Gupta', date_of_birth: '2018-06-12', age: 8, gender: 'Female', relation: 'Child', blood_group: 'O+' },
  { family_member_id: 202, full_name: 'Sunita Gupta', date_of_birth: '1962-02-28', age: 64, gender: 'Female', relation: 'Parent', blood_group: 'B+', phone: '+91 98111 22334' },
]

// family_member_id / patient_name / relation are absent on the account holder's
// own documents - the server drops them (NON_NULL), so the UI must read "mine"
// from the field being missing rather than comparing ids.
export const medicalRecords = [
  { report_id: 'r-1', report_name: 'Blood Test Report', report_type: 'Lab Report', upload_date: '2026-04-20', size: '2.4 MB', patient_name: 'Aarav Gupta' },
  { report_id: 'r-2', report_name: 'X-Ray Chest', report_type: 'Imaging', upload_date: '2026-03-15', size: '5.1 MB', patient_name: 'Aarav Gupta' },
  { report_id: 'r-3', report_name: 'Vaccination Card', report_type: 'Document', upload_date: '2026-04-18', size: '0.6 MB', family_member_id: 201, patient_name: 'Ananya Gupta', relation: 'Child' },
  { report_id: 'r-4', report_name: 'ECG - Cardiology', report_type: 'Lab Report', upload_date: '2026-02-11', size: '1.9 MB', family_member_id: 202, patient_name: 'Sunita Gupta', relation: 'Parent' },
  { report_id: 'r-5', report_name: 'Medical History', report_type: 'Document', upload_date: '2026-01-05', size: '1.2 MB', patient_name: 'Aarav Gupta' },
]

// A second opinion is its own document, not a prescription — no medicines, a
// verdict on someone else's diagnosis instead.
export const medicalOpinions = [
  {
    opinion_id: 'o-1',
    appointment_id: 'a-4',
    patient: 'Aarav Gupta',
    doctor: 'Dr. Vikram Rao',
    specialization: 'Orthopedic',
    original_diagnosis: 'Grade II ACL tear. Reconstructive surgery advised within 6 weeks.',
    findings: 'Reviewed MRI dated 12 July 2026 and the operating surgeon’s notes. The tear is partial with the ligament in continuity, and there is no meniscal involvement. Knee stability on examination is reported as near normal.',
    agrees_with_original: false,
    verdict: 'Differs from the original diagnosis',
    recommendation: 'A 12-week supervised physiotherapy programme is reasonable before considering surgery. Reassess with a repeat MRI at 8 weeks; operate only if instability persists.',
    suggested_tests: 'Repeat MRI of the right knee after 8 weeks of physiotherapy.',
    issued_on: '21 Apr 2026',
    pdf_url: '/opinions/o-1/pdf',
  },
]

export const patientProfile = {
  patient_id: 101,
  full_name: 'Aarav Gupta',
  email: 'aarav.gupta@email.com',
  phone: '+91 98765 43210',
  date_of_birth: '1990-01-15',
  gender: 'Male',
  blood_group: 'O+',
}

// ---- Doctor portal data ----
// Field names mirror AppointmentResponse, which is what the doctor pages read
// (`patient`, `appointment_id`, `appointment_date`) - a fixture shaped like the
// mock's own older `{ id, name }` renders blank rows the moment anyone opens the
// app in its default configuration.
//
// The queue cascades: 10:00 started 25 minutes late and the schedule is
// back-to-back, so everyone behind it inherits the slip until the afternoon gap.
export const doctorTodaySchedule = [
  { appointment_id: 't-1', appointment_date: '2026-05-05', time: '10:00 AM', patient: 'Aarav Gupta', age: 35, type: 'Consultation', status: 'confirmed', queue_position: 1, eta_minutes: 0, delay_minutes: 25 },
  { appointment_id: 't-2', appointment_date: '2026-05-05', time: '11:30 AM', patient: 'Kavya Reddy', age: 28, type: 'Follow-up', status: 'confirmed', queue_position: 2, eta_minutes: 55, delay_minutes: 25 },
  { appointment_id: 't-3', appointment_date: '2026-05-05', time: '02:00 PM', patient: 'Arjun Singh', age: 52, type: 'New Patient', status: 'confirmed', queue_position: 3, eta_minutes: 205 },
  { appointment_id: 't-4', appointment_date: '2026-05-05', time: '03:30 PM', patient: 'Neha Kapoor', age: 41, type: 'Consultation', status: 'confirmed', queue_position: 4, eta_minutes: 295 },
]

// No queue fields: these are past their slot time, so they have left the queue.
export const doctorPendingRequests = [
  { appointment_id: 'p-1', patient: 'Karthik Menon', age: 45, appointment_date: '2026-05-06', time: '10:00 AM', type: 'Consultation', reason: 'Chest pain consultation' },
  { appointment_id: 'p-2', patient: 'Ananya Rao', age: 33, appointment_date: '2026-05-07', time: '02:30 PM', type: 'Follow-up', reason: 'Regular checkup' },
  { appointment_id: 'p-3', patient: 'Rohit Kulkarni', age: 58, appointment_date: '2026-05-08', time: '11:00 AM', type: 'Consultation', reason: 'Blood pressure monitoring' },
]

export const doctorCompletedConsults = [
  { appointment_id: 'c-1', patient: 'Vivaan Shah', age: 39, appointment_date: '2026-05-05', time: '09:00 AM', type: 'Consultation', diagnosis: 'Viral infection', prescription: true },
  { appointment_id: 'c-2', patient: 'Divya Pillai', age: 47, appointment_date: '2026-05-05', time: '08:00 AM', type: 'Follow-up', diagnosis: 'Routine checkup', prescription: false },
  { appointment_id: 'c-3', patient: 'Kavya Reddy', age: 31, appointment_date: '2026-05-05', time: '07:30 AM', type: 'Consultation', diagnosis: '', prescription: false, no_show_by: 'PATIENT' },
]

export const doctorPatientRecords = [
  { id: 'pr-1', name: 'Aarav Gupta', age: 35, last_visit: '2026-04-20', condition: 'Hypertension', next: '2026-05-05' },
  { id: 'pr-2', name: 'Kavya Reddy', age: 28, last_visit: '2026-04-15', condition: 'Skin allergy', next: '2026-05-05' },
  { id: 'pr-3', name: 'Arjun Singh', age: 52, last_visit: 'New Patient', condition: 'N/A', next: '2026-05-05' },
]

export const doctorSchedule = [
  { day: 'Monday', available: true, morning: true, afternoon: true },
  { day: 'Tuesday', available: true, morning: true, afternoon: false },
  { day: 'Wednesday', available: true, morning: true, afternoon: true },
  { day: 'Thursday', available: true, morning: true, afternoon: true },
  { day: 'Friday', available: true, morning: true, afternoon: false },
]

export const doctorProfile = {
  full_name: 'Dr. Aditya Nair',
  specialization: 'Cardiology',
  license_number: 'MD-12345-2020',
  experience_years: 15,
  email: 'sarah.johnson@medibridge.com',
  phone: '+91 98765 43210',
  bio: 'Experienced cardiologist with 15 years of practice. Specialized in preventive cardiology and heart disease management.',
  consultation_fee: 800,
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
  { id: 'ac-1', name: 'Neha Kapoor', text: 'New patient registered', time: '2 hours ago', type: 'patient' },
  { id: 'ac-2', name: 'Dr. Aditya Nair', text: 'Completed consultation with Aarav Gupta', time: '3 hours ago', type: 'consult' },
  { id: 'ac-3', name: 'Dr. Vikram Rao', text: 'Doctor account approved', time: '5 hours ago', type: 'doctor' },
  { id: 'ac-4', name: 'Kavya Reddy', text: 'Cancelled appointment', time: '6 hours ago', type: 'cancel' },
]

export const adminPatients = [
  { patient_id: 1, full_name: 'Aarav Gupta', email: 'aarav.gupta@email.com', phone: '+91 98765 43210', join_date: '2026-01-15', appointments: 8, status: 'active' },
  { patient_id: 2, full_name: 'Kavya Reddy', email: 'kavya.r@email.com', phone: '+91 98765 43211', join_date: '2026-02-20', appointments: 5, status: 'active' },
  { patient_id: 3, full_name: 'Arjun Singh', email: 'arjun.s@email.com', phone: '+91 98765 43212', join_date: '2026-03-10', appointments: 2, status: 'active' },
  { patient_id: 4, full_name: 'Neha Kapoor', email: 'neha.k@email.com', phone: '+91 98765 43213', join_date: '2026-04-05', appointments: 0, status: 'inactive' },
]

export const adminDoctors = [
  { doctor_id: 'd-1', full_name: 'Dr. Aditya Nair', email: 'aditya.n@medibridge.com', specialization: 'Cardiologist', license_number: 'MD-12345-2020', patients: 156, status: 'active' },
  { doctor_id: 'd-2', full_name: 'Dr. Rohan Mehta', email: 'rohan.m@medibridge.com', specialization: 'Dermatologist', license_number: 'MD-12346-2020', patients: 98, status: 'active' },
  { doctor_id: 'd-3', full_name: 'Dr. Meera Joshi', email: 'meera.j@medibridge.com', specialization: 'General Physician', license_number: 'MD-12347-2020', patients: 134, status: 'active' },
  { doctor_id: 'd-4', full_name: 'Dr. Vikram Rao', email: 'robert.w@medibridge.com', specialization: 'Orthopedic', license_number: 'MD-12348-2020', patients: 87, status: 'suspended' },
]

export const adminAppointments = [
  { appointment_id: 1, patient: 'Aarav Gupta', doctor: 'Dr. Aditya Nair', date: '2026-05-05', time: '10:00 AM', type: 'Consultation', status: 'confirmed' },
  { appointment_id: 2, patient: 'Kavya Reddy', doctor: 'Dr. Rohan Mehta', date: '2026-05-05', time: '11:30 AM', type: 'Follow-up', status: 'confirmed' },
  { appointment_id: 3, patient: 'Arjun Singh', doctor: 'Dr. Meera Joshi', date: '2026-05-06', time: '02:00 PM', type: 'New Patient', status: 'pending' },
  { appointment_id: 4, patient: 'Neha Kapoor', doctor: 'Dr. Vikram Rao', date: '2026-05-07', time: '03:30 PM', type: 'Consultation', status: 'cancelled' },
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

// Mirrors app/guardrails/emergency_rules.py - hard bypass, same message,
// so mock and live behave the same for the one path that matters most.
export const chatEmergencyKeywords = ["chest pain", "can't breathe", 'cannot breathe', 'stroke', 'unconscious', 'suicidal']
export const chatEmergencyMessage = 'This may be a medical emergency. Please call your local emergency number or go to the nearest emergency room immediately. This chatbot cannot help with emergencies.'

// Mirrors the FAQ docs in medibridge-chat-service/app/knowledge/app_faq/ -
// keeps the mock chat answer roughly on-topic instead of one canned line.
export const chatFaqTopics = [
  { keywords: ['book', 'appointment', 'slot'], answer: 'Go to Find Doctors, pick a doctor and an open slot, then confirm payment - your booking is instant once payment succeeds.' },
  { keywords: ['reschedul'], answer: "You can reschedule from Appointments up to 2 hours before the slot. If your doctor reschedules instead, you'll get a notification with the new time to accept." },
  { keywords: ['no-show', 'no show', 'refund', 'missed'], answer: "If a doctor misses a confirmed slot, you're automatically refunded in full. Patient no-shows are not refunded - please cancel ahead of time instead." },
  { keywords: ['video', 'teleconsult', 'join', 'call'], answer: "The Join button on your appointment lights up 10 minutes before your slot. After the call, your doctor's prescription PDF appears in Medical Records." },
]

// Mirrors app/guardrails/specialty_rules.py's keyword table (specialty_mapping.json).
export const chatTriageRules = [
  { keywords: ['chest pain', 'shortness of breath', 'heart'], specialty: 'Cardiology', urgency: 'urgent' },
  { keywords: ['skin', 'rash', 'itching', 'acne'], specialty: 'Dermatology', urgency: 'routine' },
  { keywords: ['child', 'infant', 'toddler'], specialty: 'Pediatrics', urgency: 'routine' },
  { keywords: ['joint', 'back pain', 'fracture'], specialty: 'Orthopedics', urgency: 'routine' },
  { keywords: ['headache', 'migraine', 'dizz'], specialty: 'Neurology', urgency: 'routine' },
  { keywords: ['anxiety', 'depress', 'stress'], specialty: 'Psychiatry', urgency: 'routine' },
]
