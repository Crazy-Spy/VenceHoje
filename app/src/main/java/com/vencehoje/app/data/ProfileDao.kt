package com.vencehoje.app.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface ProfileDao {
    // Traz todos os perfis para você mostrar no menu lateral ou no topo da Home
    @Query("SELECT * FROM profiles ORDER BY id ASC")
    fun getAllProfiles(): Flow<List<Profile>>

    // Busca os dados de um perfil específico (pra saber o nome e a cor do perfil atual)
    @Query("SELECT * FROM profiles WHERE id = :id LIMIT 1")
    suspend fun getProfileById(id: Int): Profile?

    // Cria um novo perfil (Ex: "Casa do Pai")
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProfile(profile: Profile): Long

    // Atualiza nome ou cor do perfil
    @Update
    suspend fun updateProfile(profile: Profile)

    // Deleta um perfil
    // Nota mental: No futuro, teremos que decidir se apagar o perfil apaga as contas dele também.
    @Delete
    suspend fun deleteProfile(profile: Profile)
}