-- Локальный сид: 10 демо-проектов. Не часть Flyway — запускать вручную.
-- Пример (из корня репозитория, подставьте user/db из .env):
--   psql -h localhost -p 5400 -U Resume -d resume -f resume-website-backend/scripts/dev/site_project_demo_seed.sql
--
-- Пропускается, если в site_projects уже есть записи.

CREATE EXTENSION IF NOT EXISTS pgcrypto;

DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM site_projects LIMIT 1) THEN
        RAISE NOTICE 'site_projects не пуста — сид пропущен';
        RETURN;
    END IF;

    INSERT INTO skills (name, created_at, updated_at)
    VALUES
        ('React', NOW(), NOW()),
        ('Java', NOW(), NOW()),
        ('PostgreSQL', NOW(), NOW()),
        ('Unity', NOW(), NOW()),
        ('C#', NOW(), NOW()),
        ('Flutter', NOW(), NOW()),
        ('Dart', NOW(), NOW()),
        ('JavaScript', NOW(), NOW()),
        ('Python', NOW(), NOW()),
        ('Docker', NOW(), NOW()),
        ('Git', NOW(), NOW()),
        ('MQTT', NOW(), NOW())
    ON CONFLICT (name) DO NOTHING;

    INSERT INTO site_projects (
        id, title, section, summary, body, sort_order, visible_to_anonymous, created_at, updated_at
    )
    VALUES
        (
            'aaaaaaaa-0001-4000-8000-000000000001'::uuid,
            'GameCheb',
            'Игры',
            'Мобильная RPG по мотивам Чебоксар',
            'Командный дипломный проект: приключенческая RPG с локальными достопримечательностями и квестами по городу.',
            0, TRUE, NOW(), NOW()
        ),
        (
            'aaaaaaaa-0001-4000-8000-000000000002'::uuid,
            'Singularity Resume',
            'Веб-разработка',
            'Платформа резюме и стажировок',
            'Full-stack сервис для студентов и работодателей: профили, вакансии, чаты и модерация контента.',
            1, TRUE, NOW(), NOW()
        ),
        (
            'aaaaaaaa-0001-4000-8000-000000000003'::uuid,
            'VR Lab',
            'VR / AR',
            'Виртуальная лаборатория химии',
            'Иммерсивный тренажёр для колледжа: безопасные эксперименты в VR с подсказками преподавателя.',
            2, TRUE, NOW(), NOW()
        ),
        (
            'aaaaaaaa-0001-4000-8000-000000000004'::uuid,
            'EcoTrack',
            'Мобильная разработка',
            'Трекер экологичных привычек',
            'Flutter-приложение с геймификацией: баллы за сортировку отходов и общественный рейтинг групп.',
            3, TRUE, NOW(), NOW()
        ),
        (
            'aaaaaaaa-0001-4000-8000-000000000005'::uuid,
            'CodeQuest',
            'Образование',
            'Интерактивный курс по алгоритмам',
            'Веб-платформа с задачами, визуализацией шагов и интеграцией с GitHub Classroom.',
            4, TRUE, NOW(), NOW()
        ),
        (
            'aaaaaaaa-0001-4000-8000-000000000006'::uuid,
            'SmartGreenhouse',
            'IoT',
            'Умная теплица для campus-проекта',
            'Датчики влажности и освещения, автоматический полив и дашборд на React + MQTT.',
            5, TRUE, NOW(), NOW()
        ),
        (
            'aaaaaaaa-0001-4000-8000-000000000007'::uuid,
            'PixelForge',
            'Дизайн',
            'Брендинг локального фестиваля',
            'Айдентика, постеры и motion-ролик для молодёжного фестиваля медиа и технологий.',
            6, TRUE, NOW(), NOW()
        ),
        (
            'aaaaaaaa-0001-4000-8000-000000000008'::uuid,
            'NeuroAssist',
            'Искусственный интеллект',
            'Чат-бот поддержки абитуриентов',
            'RAG-ассистент на базе FAQ колледжа: ответы о специальностях, документах и сроках подачи.',
            7, TRUE, NOW(), NOW()
        ),
        (
            'aaaaaaaa-0001-4000-8000-000000000009'::uuid,
            'SoundMap',
            'Аудио',
            'Карта городских звуков',
            'Коллекция field-recording с интерактивной картой и Web Audio API для прослушивания точек.',
            8, TRUE, NOW(), NOW()
        ),
        (
            'aaaaaaaa-0001-4000-8000-00000000000a'::uuid,
            'DevOps Sprint',
            'DevOps',
            'CI/CD для учебных репозиториев',
            'Шаблон пайплайна: тесты, линтеры, deploy preview и уведомления в Telegram для команд.',
            9, TRUE, NOW(), NOW()
        );

    INSERT INTO site_project_images (id, site_project_id, image_url, sort_order)
    VALUES
        (gen_random_uuid(), 'aaaaaaaa-0001-4000-8000-000000000001'::uuid, 'https://picsum.photos/seed/sin-project-0/800/500', 0),
        (gen_random_uuid(), 'aaaaaaaa-0001-4000-8000-000000000002'::uuid, 'https://picsum.photos/seed/sin-project-1/800/500', 0),
        (gen_random_uuid(), 'aaaaaaaa-0001-4000-8000-000000000003'::uuid, 'https://picsum.photos/seed/sin-project-2/800/500', 0),
        (gen_random_uuid(), 'aaaaaaaa-0001-4000-8000-000000000004'::uuid, 'https://picsum.photos/seed/sin-project-3/800/500', 0),
        (gen_random_uuid(), 'aaaaaaaa-0001-4000-8000-000000000005'::uuid, 'https://picsum.photos/seed/sin-project-4/800/500', 0),
        (gen_random_uuid(), 'aaaaaaaa-0001-4000-8000-000000000006'::uuid, 'https://picsum.photos/seed/sin-project-5/800/500', 0),
        (gen_random_uuid(), 'aaaaaaaa-0001-4000-8000-000000000007'::uuid, 'https://picsum.photos/seed/sin-project-6/800/500', 0),
        (gen_random_uuid(), 'aaaaaaaa-0001-4000-8000-000000000008'::uuid, 'https://picsum.photos/seed/sin-project-7/800/500', 0),
        (gen_random_uuid(), 'aaaaaaaa-0001-4000-8000-000000000009'::uuid, 'https://picsum.photos/seed/sin-project-8/800/500', 0),
        (gen_random_uuid(), 'aaaaaaaa-0001-4000-8000-00000000000a'::uuid, 'https://picsum.photos/seed/sin-project-9/800/500', 0);

    INSERT INTO site_project_skills (site_project_id, skill_id)
    SELECT v.project_id, s.id
    FROM (VALUES
        ('aaaaaaaa-0001-4000-8000-000000000001'::uuid, 'Unity'),
        ('aaaaaaaa-0001-4000-8000-000000000001'::uuid, 'C#'),
        ('aaaaaaaa-0001-4000-8000-000000000002'::uuid, 'React'),
        ('aaaaaaaa-0001-4000-8000-000000000002'::uuid, 'Java'),
        ('aaaaaaaa-0001-4000-8000-000000000002'::uuid, 'PostgreSQL'),
        ('aaaaaaaa-0001-4000-8000-000000000003'::uuid, 'Unity'),
        ('aaaaaaaa-0001-4000-8000-000000000003'::uuid, 'C#'),
        ('aaaaaaaa-0001-4000-8000-000000000004'::uuid, 'Flutter'),
        ('aaaaaaaa-0001-4000-8000-000000000004'::uuid, 'Dart'),
        ('aaaaaaaa-0001-4000-8000-000000000005'::uuid, 'JavaScript'),
        ('aaaaaaaa-0001-4000-8000-000000000005'::uuid, 'Python'),
        ('aaaaaaaa-0001-4000-8000-000000000006'::uuid, 'React'),
        ('aaaaaaaa-0001-4000-8000-000000000006'::uuid, 'MQTT'),
        ('aaaaaaaa-0001-4000-8000-000000000006'::uuid, 'JavaScript'),
        ('aaaaaaaa-0001-4000-8000-000000000007'::uuid, 'JavaScript'),
        ('aaaaaaaa-0001-4000-8000-000000000008'::uuid, 'Python'),
        ('aaaaaaaa-0001-4000-8000-000000000009'::uuid, 'JavaScript'),
        ('aaaaaaaa-0001-4000-8000-00000000000a'::uuid, 'Docker'),
        ('aaaaaaaa-0001-4000-8000-00000000000a'::uuid, 'Git')
    ) AS v(project_id, skill_name)
    JOIN skills s ON s.name = v.skill_name
    ON CONFLICT (site_project_id, skill_id) DO NOTHING;

    RAISE NOTICE 'Демо-проекты добавлены';
END $$;
