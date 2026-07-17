create table public.question_favorites
(
    id           bigserial,
    user_id      bigint                      not null,
    question_ids bigint[]                    not null,
    create_at    timestamp without time zone not null,
    constraint question_favorites_pk
        unique (user_id, question_ids)
);

comment on table public.question_favorites is '题目收藏表';

comment on column public.question_favorites.id is '主键';

comment on column public.question_favorites.user_id is '关联用户表的user_id';

comment on column public.question_favorites.question_ids is '关联questions表的question_id';

comment on column public.question_favorites.create_at is '创建时间';