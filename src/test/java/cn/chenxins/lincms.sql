create table drive.lin_user
(
    id            int auto_increment
        primary key,
    create_time   timestamp   null,
    update_time   timestamp   null,
    delete_time   timestamp   null,
    nickname      varchar(64) null,
    card          varchar(32) null,
    phone         varchar(32) null,
    cost          double      null,
    coach_id      int         null,
    introducer    varchar(32) null,
    subject_one   int         null,
    subject_two   int         null,
    register_time timestamp   null,
    type          int         null
);

create table drive.ticket
(
    id          int auto_increment
        primary key,
    student_id  int       null,
    user_count  int       null,
    coash_id    int       null,
    create_time timestamp null,
    update_time timestamp null,
    delete_time timestamp null,
    constraint ticket_id_uindex
        unique (id)
);

insert into lin_user (id, nickname, card, phone,  type)
values (1,'admin','10086','10086',0);
