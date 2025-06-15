package di

import controllers.*
import repositories.*
import services.*
import utils.PasswordHasher
import org.koin.dsl.module

/**
 * Modules Koin pour l'injection de dépendances Arka
 *
 * Architecture modulaire pour une maintenance facile :
 * - repositoryModule : Couche d'accès aux données
 * - serviceModule : Logique métier et orchestration
 * - controllerModule : Interface entre UI et services
 * - utilityModule : Outils et utilitaires
 */

/**
 * Module des repositories - Couche d'accès aux données
 */
val repositoryModule = module {
    // Repositories familiaux
    single { FamilyRepository() }
    single { FamilyMemberRepository() }

    // Repositories de contenu
    single { CategoryRepository() }
    single { FolderRepository() }
    single { FileRepository() }
    single { DefaultFolderTemplateRepository() }

    // Repositories de permissions
    single { DelegationRequestRepository() }
    single { PermissionRepository() }
    single { JournalAuditPermissionRepository() }

    // ✅ REPOSITORY ALERT - FONCTIONNEL
    single { AlertRepository() }

    // Repositories d'espaces (quand créés)
    // single { SpaceRepository() }
    // single { MemberSpaceRepository() }
}

/**
 * Module des services - Logique métier
 */
val serviceModule = module {
    // Services de base (pas de dépendances complexes)
    single { NavigationService() }

    // Services avec dépendances simples
    single { SessionService(get()) }  // Dépend d'AuthController
    single { HealthService(get()) }   // Dépend de FamilyRepository

    // Services avec dépendances multiples (après les controllers)
    // Note: DashboardService et SearchService seront ajoutés après création des controllers manquants
}

/**
 * Module des controllers - Interface avec l'UI
 */
val controllerModule = module {
    // Controllers de base (prêts et testés)
    single { AuthController(get(), get()) }        // FamilyMemberRepository + PasswordHasher
    single { FamilyController(get(), get()) }      // FamilyRepository + FamilyMemberRepository
    single { FamilyMemberController(get(), get(), get()) } // FamilyMemberRepository + FamilyRepository + PasswordHasher

    // ✅ CONTROLLERS PERMISSIONS ET ALERTES - FONCTIONNELS
    single { PermissionController(get(), get(), get(), get()) } // PermissionRepository + FamilyMemberRepository + FileRepository + FolderRepository
    single { AlertController(get(), get()) }                   // AlertRepository + AuthController
    single { DelegationController(get(), get(), get()) }       // DelegationRequestRepository + PermissionRepository + FamilyMemberRepository

    // ✅ CONTROLLER AUDIT - FONCTIONNEL
    single { JournalAuditPermissionController(get()) }         // JournalAuditPermissionRepository

    // Controllers de contenu (à activer quand créés)
    // single { CategoryController(get()) }
    // single { FolderController(get(), get(), get()) }
    // single { FileController(get(), get(), get()) }

    // Controller principal - VERSION MINIMALE (3 services seulement)
    single { MainController(get(), get(), get()) } // SessionService + NavigationService + HealthService
}

/**
 * Module des utilitaires - Outils et helpers
 */
val utilityModule = module {
    // PasswordHasher est un object singleton, mais on peut l'injecter pour les tests
    single { PasswordHasher }

    // Autres utilitaires
    // single { DateFormatter() }
    // single { FileTypeDetector() }
    // single { ValidationUtils() }
}

/**
 * Liste complète des modules Arka
 * Ordre important : dependencies d'abord, puis dependents
 */
val arkaModules = listOf(
    utilityModule,      // Pas de dépendances
    repositoryModule,   // Dépend seulement de utilityModule
    serviceModule,      // Dépend de repositoryModule et utilityModule
    controllerModule    // Dépend de tout
)