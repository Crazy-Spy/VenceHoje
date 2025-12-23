package com.vencehoje.app.data

import kotlinx.coroutines.flow.Flow

class BillRepository(
    private val billDao: BillDao,
    private val categoryDao: CategoryDao,
    private val profileDao: ProfileDao // Injetamos o novo DAO aqui
) {

    // --- LÓGICA DE PERFIS (PROFILES) ---

    // Lista todos os perfis disponíveis (para o menu de troca)
    val allProfiles: Flow<List<Profile>> = profileDao.getAllProfiles()

    suspend fun getProfileById(id: Int): Profile? {
        return profileDao.getProfileById(id)
    }

    suspend fun clearBillsByProfile(profileId: Int) {
        billDao.deleteBillsByProfile(profileId)
    }

    suspend fun insertProfile(profile: Profile) {
        val newProfileId = profileDao.insertProfile(profile)
        // Ciência: Ao criar um perfil novo (ex: "Casa do Pai"),
        // já populamos ele com as categorias padrão automaticamente.
        seedCategoriesForProfile(newProfileId.toInt())
    }

    suspend fun updateProfile(profile: Profile) {
        profileDao.updateProfile(profile)
    }

    suspend fun deleteProfile(profile: Profile) {
        // Cuidado: Aqui deveríamos decidir se apagamos as contas junto.
        // Por segurança, vamos apagar o perfil e as contas ficam "órfãs" ou deletamos tudo.
        // Vamos assumir deletar tudo para não sujar o banco:
        billDao.deleteBillsByProfile(profile.id)
        profileDao.deleteProfile(profile)
    }


    // --- LÓGICA DE CONTAS (BILLS) ---

    // AGORA PEDE O ID DO PERFIL!
    fun getBillsByProfile(profileId: Int): Flow<List<Bill>> {
        return billDao.getBillsByProfile(profileId)
    }

    // Método Global para o NotificationWorker (Vigia Noturno)
    suspend fun getAllBillsGlobal(): List<Bill> {
        return billDao.getAllBillsGlobalSync()
    }

    suspend fun insertBill(bill: Bill) {
        billDao.insertBill(bill)
    }

    suspend fun deleteBill(bill: Bill) {
        billDao.deleteBill(bill)
    }

    suspend fun updateBill(bill: Bill) {
        billDao.updateBill(bill)
    }


    // --- LÓGICA DE CATEGORIAS ---

    // AGORA PEDE O ID DO PERFIL!
    fun getCategoriesByProfile(profileId: Int): Flow<List<Category>> {
        return categoryDao.getCategoriesByProfile(profileId)
    }

    // Método Global para pegar emojis no NotificationWorker
    suspend fun getAllCategoriesGlobal(): List<Category> {
        return categoryDao.getAllCategoriesGlobalSync()
    }

    suspend fun insertCategory(category: Category) {
        categoryDao.insertCategory(category)
    }

    suspend fun deleteCategory(category: Category) {
        categoryDao.deleteCategory(category)
    }

    /**
     * Carga Inicial Inteligente:
     * Verifica se o Perfil Principal (ID 1) tem categorias. Se não, cria.
     * Também usamos essa lógica auxiliar para novos perfis.
     */
    suspend fun checkAndSeedCategories(profileId: Int = 1) {
        // Pega as categorias globais e filtra na memória para ser rápido e síncrono
        val allCats = categoryDao.getAllCategoriesGlobalSync()
        val hasCategoriesForProfile = allCats.any { it.profileId == profileId }

        if (!hasCategoriesForProfile) {
            seedCategoriesForProfile(profileId)
        }
    }

    // Função auxiliar privada para criar as categorias padrão em qualquer perfil
    private suspend fun seedCategoriesForProfile(profileId: Int) {
        val defaultCategories = listOf(
            Category(name = "Moradia", colorHex = "#1976D2", iconName = "home", isBuiltIn = true, profileId = profileId),
            Category(name = "Transporte", colorHex = "#FBC02D", iconName = "directions_car", isBuiltIn = true, profileId = profileId),
            Category(name = "Saúde", colorHex = "#2FD3B2", iconName = "medical_services", isBuiltIn = true, profileId = profileId),
            Category(name = "Lazer", colorHex = "#7B1FA2", iconName = "celebration", isBuiltIn = true, profileId = profileId),
            Category(name = "Alimentação", colorHex = "#388E3C", iconName = "restaurant", isBuiltIn = true, profileId = profileId),
            Category(name = "Educação", colorHex = "#00796B", iconName = "school", isBuiltIn = true, profileId = profileId),
            Category(name = "Outros", colorHex = "#9E9E9E", iconName = "label", isBuiltIn = true, profileId = profileId)
        )
        // Usa insertAll se tiver criado no DAO, ou loop se não tiver
        defaultCategories.forEach { categoryDao.insertCategory(it) }
    }
}