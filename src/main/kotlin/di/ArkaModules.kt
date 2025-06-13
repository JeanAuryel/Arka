package di


import controllers.FamilyController
import repositories.*
import org.koin.dsl.module

/**
 * Modules Koin pour l'injection de dépendances Arka
 */

/**
 * Module des repositories
 */
val repositoryModule = module {
    // Repositories principaux
    single { FamilyRepository() }
    single { FamilyMemberRepository() }
    single { CategoryRepository() }

    // Repositories de contenu
    single { DefaultFolderTemplateRepository() }
    single { FolderRepository() }
    single { FileRepository() }

    // Repositories de délégation (système de permissions)
    single { DelegationRequestRepository() }
    single { PermissionRepository() }

    // TODO: Ajouter EspaceRepository quand créé
    // single { EspaceRepository() }
}

/**
 * Module des controllers
 */
val controllerModule = module {
    // Controllers principaux
    single { FamilyController(get(), get()) }

    // TODO: Ajouter les nouveaux controllers au fur et à mesure
    // single { FileController(get(), get(), get()) }
    // single { FolderController(get(), get(), get()) }
    // single { PermissionController(get(), get()) }
    // single { DelegationController(get(), get(), get()) }
}

/**
 * Module des utilitaires
 */
val utilityModule = module {
    // Utilitaires disponibles en singleton
    // PasswordHasher est déjà un object singleton, pas besoin de l'injecter
}

/**
 * Liste de tous les modules Arka
 */
val arkaModules = listOf(
    repositoryModule,
    controllerModule,
    utilityModule
)package com.arka.di

import com.arka.controllers.FamilyController
import com.arka.repositories.*
import org.koin.dsl.module

/**
 * Modules Koin pour l'injection de dépendances Arka
 */

/**
 * Module des repositories
 */
val repositoryModule = module {
    // Repositories principaux
    single { FamilyRepository() }
    single { FamilyMemberRepository() }
    single { CategoryRepository() }

    // Repositories de contenu
    single { DefaultFolderTemplateRepository() }
    single { FolderRepository() }
    single { FileRepository() }

    // Repositories de délégation (système de permissions)
    single { DelegationRequestRepository() }
    single { PermissionRepository() }

    // TODO: Ajouter EspaceRepository quand créé
    // single { EspaceRepository() }
}

/**
 * Module des controllers
 */
val controllerModule = module {
    // Controllers principaux
    single { FamilyController(get(), get()) }

    // TODO: Ajouter les nouveaux controllers au fur et à mesure
    // single { FileController(get(), get(), get()) }
    // single { FolderController(get(), get(), get()) }
    // single { PermissionController(get(), get()) }
    // single { DelegationController(get(), get(), get()) }
}

/**
 * Module des utilitaires
 */
val utilityModule = module {
    // Utilitaires disponibles en singleton
    // PasswordHasher est déjà un object singleton, pas besoin de l'injecter
}

/**
 * Liste de tous les modules Arka
 */
val arkaModules = listOf(
    repositoryModule,
    controllerModule,
    utilityModule
)
