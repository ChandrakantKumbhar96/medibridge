import axiosClient, { USE_MOCK } from '../api/axiosClient'
import { mockResolve } from './_mock'
import { familyMembers } from '../api/mock/mockData'

/**
 * Dependent profiles on the signed-in patient's account.
 *
 * No method takes an owner id: the server reads it from the JWT and every
 * lookup is by (family_member_id, owner). Sending one from here would be
 * decorative at best and misleading at worst.
 */
export const familyService = {
  async getFamily() {
    if (USE_MOCK) return mockResolve(familyMembers)
    const { data } = await axiosClient.get('/patient/family')
    return data
  },

  async addMember(member) {
    if (USE_MOCK) {
      return mockResolve({ ...member, family_member_id: `fm-${Date.now()}`, age: ageOf(member.date_of_birth) })
    }
    const { data } = await axiosClient.post('/patient/family', member)
    return data
  },

  async updateMember(familyMemberId, member) {
    if (USE_MOCK) {
      return mockResolve({ ...member, family_member_id: familyMemberId, age: ageOf(member.date_of_birth) })
    }
    const { data } = await axiosClient.put(`/patient/family/${familyMemberId}`, member)
    return data
  },

  /**
   * Removes the profile from the booking dropdown. The server archives rather
   * than deletes — their appointments and reports stay readable, which is also
   * the only thing the foreign keys allow.
   */
  async removeMember(familyMemberId) {
    if (USE_MOCK) return mockResolve({ family_member_id: familyMemberId })
    await axiosClient.delete(`/patient/family/${familyMemberId}`)
    return { family_member_id: familyMemberId }
  },
}

function ageOf(dateOfBirth) {
  if (!dateOfBirth) return null
  const dob = new Date(dateOfBirth)
  const now = new Date()
  let age = now.getFullYear() - dob.getFullYear()
  const m = now.getMonth() - dob.getMonth()
  if (m < 0 || (m === 0 && now.getDate() < dob.getDate())) age -= 1
  return age
}
