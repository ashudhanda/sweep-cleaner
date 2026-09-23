package com.sweep.cleaner.util

import com.sweep.cleaner.model.ContactItem
import com.sweep.cleaner.model.DuplicateContactGroup
import java.util.Locale
import java.util.UUID

object ContactUtils {

    /**
     * Normalizes a phone number by stripping whitespace, dashes, parentheses,
     * and leading international prefix symbols to retain core digits.
     */
    fun normalizePhoneNumber(phone: String): String {
        val digits = phone.filter { it.isDigit() }
        // Keep last 10 digits for local matching across international dial formats
        return if (digits.length > 10) digits.takeLast(10) else digits
    }

    /**
     * Normalizes contact name by trimming, lowercasing, and removing punctuation.
     */
    fun normalizeName(name: String): String {
        return name.trim().lowercase(Locale.ROOT).replace(Regex("[^a-z0-9 ]"), "")
    }

    /**
     * Groups contacts into duplicate groups if they share identical normalized names,
     * or any matching normalized phone number (min 7 digits), or matching email.
     */
    fun findDuplicateContacts(contacts: List<ContactItem>): List<DuplicateContactGroup> {
        val visited = BooleanArray(contacts.size)
        val groups = mutableListOf<DuplicateContactGroup>()

        for (i in contacts.indices) {
            if (visited[i]) continue
            val primary = contacts[i]
            val normPrimaryName = normalizeName(primary.displayName)
            val normPrimaryPhones = primary.phoneNumbers.map { normalizePhoneNumber(it) }.filter { it.length >= 7 }.toSet()
            val primaryEmails = primary.emails.map { it.trim().lowercase(Locale.ROOT) }.filter { it.isNotEmpty() }.toSet()

            val duplicates = mutableListOf<ContactItem>()

            for (j in i + 1 until contacts.size) {
                if (visited[j]) continue
                val candidate = contacts[j]
                val normCandName = normalizeName(candidate.displayName)
                val normCandPhones = candidate.phoneNumbers.map { normalizePhoneNumber(it) }.filter { it.length >= 7 }.toSet()
                val candEmails = candidate.emails.map { it.trim().lowercase(Locale.ROOT) }.filter { it.isNotEmpty() }.toSet()

                val nameMatches = normPrimaryName.isNotBlank() && normPrimaryName == normCandName
                val phoneMatches = normPrimaryPhones.intersect(normCandPhones).isNotEmpty()
                val emailMatches = primaryEmails.intersect(candEmails).isNotEmpty()

                if (nameMatches || phoneMatches || emailMatches) {
                    visited[j] = true
                    duplicates.add(candidate)
                }
            }

            if (duplicates.isNotEmpty()) {
                visited[i] = true
                val allPhones = (primary.phoneNumbers + duplicates.flatMap { it.phoneNumbers })
                    .distinctBy { normalizePhoneNumber(it) }
                val allEmails = (primary.emails + duplicates.flatMap { it.emails })
                    .distinctBy { it.trim().lowercase(Locale.ROOT) }

                groups.add(
                    DuplicateContactGroup(
                        groupId = UUID.randomUUID().toString(),
                        primaryContact = primary,
                        duplicates = duplicates,
                        survivingPhoneNumbers = allPhones,
                        survivingEmails = allEmails
                    )
                )
            }
        }

        return groups
    }
}
