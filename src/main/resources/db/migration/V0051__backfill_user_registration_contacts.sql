UPDATE users u
SET registration_phone = COALESCE(u.registration_phone, s.phone_number),
    registration_email = COALESCE(u.registration_email, s.email)
FROM students s
WHERE u.student_id = s.id
  AND (u.registration_phone IS NULL OR u.registration_email IS NULL);
