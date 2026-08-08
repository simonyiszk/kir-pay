INSERT INTO principals (id, name, secret, role, active, can_upload, can_transfer, can_sell_items, can_redeem_vouchers,
                        can_assign_cards, created_at, last_used)
VALUES (1, 'Isti', '$2a$10$dkQIZE3l1GRjCwHecLT29O1GvpH/ZGaab4a/8g4yVnOFNtxOVFuGG', 'ADMIN', true, true, true, true,
        true, true, 1724234684953, 1724236016046);
INSERT INTO principals (id, name, secret, role, active, can_upload, can_transfer, can_sell_items, can_redeem_vouchers,
                        can_assign_cards, created_at, last_used)
VALUES (2, 'Pultos1', '$2a$10$AMfhBOwptauy3LhtcKQ7VOHA5ZqEqOpsK3MyIdQs8HfdoKx8MD85K', 'TERMINAL', true, true, true,
        true, true, true, 1724234714216, 1724236152252);

INSERT INTO accounts (id, name, email, phone, card, balance, active)
VALUES (3, 'Example Elemér', null, '69-420-13-37', null, 0, true);
INSERT INTO accounts (id, name, email, phone, card, balance, active)
VALUES (1, 'Horváth István', 'isti@simonyi.bme.hu', '+123456789', '72:b2:29:4b', 5000, true);
INSERT INTO accounts (id, name, email, phone, card, balance, active)
VALUES (2, 'Példa Petra', 'petra@gmail.com', null, '78:48:69:4a', 3500, true);

INSERT INTO items (id, name, alias, cost, stock, enabled)
VALUES (3, 'Unicum', 'fuj', 999, 0, true);
INSERT INTO items (id, name, alias, cost, stock, enabled)
VALUES (4, 'Unicum Barista', null, 999, 100000, false);
INSERT INTO items (id, name, alias, cost, stock, enabled)
VALUES (2, 'Somersby Apple', 'alma', 500, 99998, true);
INSERT INTO items (id, name, alias, cost, stock, enabled)
VALUES (1, 'Sör', null, 400, 99997, true);

INSERT INTO orders (id, account_id, timestamp)
VALUES (1, 1, 1724235048238);
INSERT INTO orders (id, account_id, timestamp)
VALUES (15, 1, 1724235886160);
INSERT INTO orders (id, account_id, timestamp)
VALUES (18, 1, 1724235934316);

INSERT INTO vouchers (id, account_id, item_id, count)
VALUES (1, 2, 3, 100);
INSERT INTO vouchers (id, account_id, item_id, count)
VALUES (2, 1, 1, 1);

INSERT INTO order_lines (id, order_id, item_id, item_count, message, used_voucher, paid_amount)
VALUES (1, 1, 2, 2, null, false, 1000);
INSERT INTO order_lines (id, order_id, item_id, item_count, message, used_voucher, paid_amount)
VALUES (2, 1, 1, 1, null, false, 400);
INSERT INTO order_lines (id, order_id, item_id, item_count, message, used_voucher, paid_amount)
VALUES (3, 1, null, 1, 'Ropi', false, 200);
INSERT INTO order_lines (id, order_id, item_id, item_count, message, used_voucher, paid_amount)
VALUES (4, 15, 1, 1, null, true, 0);
INSERT INTO order_lines (id, order_id, item_id, item_count, message, used_voucher, paid_amount)
VALUES (5, 18, 1, 1, null, false, 400);

INSERT INTO transactions (id, sender_id, recipient_id, type, amount, message, timestamp, fingerprint)
VALUES (1, null, 1, 'TOP_UP', 1000, null, 1724234939192, 'seed-tx-1');
INSERT INTO transactions (id, sender_id, recipient_id, type, amount, message, timestamp, fingerprint)
VALUES (2, null, 2, 'TOP_UP', 1000, null, 1724234943816, 'seed-tx-2');
INSERT INTO transactions (id, sender_id, recipient_id, type, amount, message, timestamp, fingerprint)
VALUES (3, null, 1, 'TOP_UP', 1000, null, 1724234949424, 'seed-tx-3');
INSERT INTO transactions (id, sender_id, recipient_id, type, amount, message, timestamp, fingerprint)
VALUES (4, 2, 1, 'TRANSFER', 1000, null, 1724234962255, 'seed-tx-4');
INSERT INTO transactions (id, sender_id, recipient_id, type, amount, message, timestamp, fingerprint)
VALUES (5, 1, null, 'CHARGE', 1000, null, 1724234990553, 'seed-tx-5');
INSERT INTO transactions (id, sender_id, recipient_id, type, amount, message, timestamp, fingerprint)
VALUES (6, 1, null, 'CHARGE', 1000, 'Somersby Apple', 1724235048264, 'seed-tx-6');
INSERT INTO transactions (id, sender_id, recipient_id, type, amount, message, timestamp, fingerprint)
VALUES (7, 1, null, 'CHARGE', 400, 'Sör', 1724235048275, 'seed-tx-7');
INSERT INTO transactions (id, sender_id, recipient_id, type, amount, message, timestamp, fingerprint)
VALUES (10, 1, null, 'CHARGE', 200, 'Ropi', 1724235048281, 'seed-tx-10');
INSERT INTO transactions (id, sender_id, recipient_id, type, amount, message, timestamp, fingerprint)
VALUES (12, 1, null, 'CHARGE', 400, 'Sör', 1724235934324, 'seed-tx-12');
INSERT INTO transactions (id, sender_id, recipient_id, type, amount, message, timestamp, fingerprint)
VALUES (13, null, 2, 'TOP_UP', 5000, null, 1724235952657, 'seed-tx-13');
INSERT INTO transactions (id, sender_id, recipient_id, type, amount, message, timestamp, fingerprint)
VALUES (14, null, 1, 'TOP_UP', 5000, null, 1724235957881, 'seed-tx-14');
INSERT INTO transactions (id, sender_id, recipient_id, type, amount, message, timestamp, fingerprint)
VALUES (15, 2, null, 'CHARGE', 1500, null, 1724235972338, 'seed-tx-15');

INSERT INTO events (id, event, timestamp, message, performed_by)
VALUES (1, 'Életciklus', 1724234495281, 'Az alkalmazás elindult', 'Rendszer');
INSERT INTO events (id, event, timestamp, message, performed_by)
VALUES (2, 'Principal létrehozva', 1724234495532, 'admin | Adminisztrátor', 'Ismeretlen végrehajtó');
INSERT INTO events (id, event, timestamp, message, performed_by)
VALUES (3, 'Principal módosítva', 1724234685023, 'Isti | Adminisztrátor', 'admin (admin)');
INSERT INTO events (id, event, timestamp, message, performed_by)
VALUES (4, 'Principal létrehozva', 1724234714282, 'Pultos1 | Terminál', 'Isti (admin)');
INSERT INTO events (id, event, timestamp, message, performed_by)
VALUES (5, 'Számla létrehozva', 1724234763662, '1: Horváth István - isti@simonyi.bme.hu', 'Isti (admin)');
INSERT INTO events (id, event, timestamp, message, performed_by)
VALUES (6, 'Számla létrehozva', 1724234797299, '2: Példa Petra - petra@gmail.com', 'Isti (admin)');
INSERT INTO events (id, event, timestamp, message, performed_by)
VALUES (7, 'Számla létrehozva', 1724234818483, '3: Example Elemér', 'Isti (admin)');
INSERT INTO events (id, event, timestamp, message, performed_by)
VALUES (8, 'Termék létrehozva', 1724234839256, 'Sör: 100000 db, 400 JMF - Aktív', 'Isti (admin)');
INSERT INTO events (id, event, timestamp, message, performed_by)
VALUES (9, 'Termék létrehozva', 1724234851282, 'Somersby Apple: 100000 db, 500 JMF - Aktív', 'Isti (admin)');
INSERT INTO events (id, event, timestamp, message, performed_by)
VALUES (10, 'Termék létrehozva', 1724234865852, 'Unicum: 0 db, 999 JMF - Aktív', 'Isti (admin)');
INSERT INTO events (id, event, timestamp, message, performed_by)
VALUES (11, 'Termék létrehozva', 1724234879666, 'Unicum Barista: 100000 db, 999 JMF - Inaktív', 'Isti (admin)');
INSERT INTO events (id, event, timestamp, message, performed_by)
VALUES (12, 'Kártya hozzárendelve', 1724234909218, '1: Horváth István - isti@simonyi.bme.hu', 'Pultos1 (terminal)');
INSERT INTO events (id, event, timestamp, message, performed_by)
VALUES (13, 'Kártya hozzárendelve', 1724234927778, '2: Példa Petra - petra@gmail.com', 'Pultos1 (terminal)');
INSERT INTO events (id, event, timestamp, message, performed_by)
VALUES (14, 'Feltöltés', 1724234939192, '1: Horváth István - isti@simonyi.bme.hu | 1000 JMF', 'Pultos1 (terminal)');
INSERT INTO events (id, event, timestamp, message, performed_by)
VALUES (15, 'Feltöltés', 1724234943816, '2: Példa Petra - petra@gmail.com | 1000 JMF', 'Pultos1 (terminal)');
INSERT INTO events (id, event, timestamp, message, performed_by)
VALUES (16, 'Feltöltés', 1724234949424, '1: Horváth István - isti@simonyi.bme.hu | 1000 JMF', 'Pultos1 (terminal)');
INSERT INTO events (id, event, timestamp, message, performed_by)
VALUES (17, 'Átutalás', 1724234962255,
        '2: Példa Petra - petra@gmail.com -> 1: Horváth István - isti@simonyi.bme.hu | 1000 JMF', 'Pultos1 (terminal)');
INSERT INTO events (id, event, timestamp, message, performed_by)
VALUES (18, 'Fizetés', 1724234990553, '1: Horváth István - isti@simonyi.bme.hu | 1000 JMF', 'Pultos1 (terminal)');
INSERT INTO events (id, event, timestamp, message, performed_by)
VALUES (19, 'Számla szerkesztve', 1724235003771, '2: Példa Petra - petra@gmail.com', 'Pultos1 (terminal)');
INSERT INTO events (id, event, timestamp, message, performed_by)
VALUES (20, 'Kártya hozzárendelve', 1724235003776, '1: Horváth István - isti@simonyi.bme.hu', 'Pultos1 (terminal)');
INSERT INTO events (id, event, timestamp, message, performed_by)
VALUES (21, 'Számla szerkesztve', 1724235011752, '1: Horváth István - isti@simonyi.bme.hu', 'Pultos1 (terminal)');
INSERT INTO events (id, event, timestamp, message, performed_by)
VALUES (22, 'Kártya hozzárendelve', 1724235011755, '2: Példa Petra - petra@gmail.com', 'Pultos1 (terminal)');
INSERT INTO events (id, event, timestamp, message, performed_by)
VALUES (23, 'Kártya hozzárendelve', 1724235016498, '1: Horváth István - isti@simonyi.bme.hu', 'Pultos1 (terminal)');
INSERT INTO events (id, event, timestamp, message, performed_by)
VALUES (24, 'Fizetés', 1724235048256, '1: Horváth István - isti@simonyi.bme.hu | 1000 JMF', 'Pultos1 (terminal)');
INSERT INTO events (id, event, timestamp, message, performed_by)
VALUES (25, 'Fizetés', 1724235048273, '1: Horváth István - isti@simonyi.bme.hu | 400 JMF', 'Pultos1 (terminal)');
INSERT INTO events (id, event, timestamp, message, performed_by)
VALUES (26, 'Rendelés létrehozva', 1724235048243, 'Rendelésazonosító: 1 - Számlaazonosító: 1', 'Pultos1 (terminal)');
INSERT INTO events (id, event, timestamp, message, performed_by)
VALUES (27, 'Termék eladva', 1724235048281,
        'Rendelésazonosító: 1 - Számlaazonosító: 1 | Mennyiség: 1, Fizetve: 200, Termék: Ropi', 'Pultos1 (terminal)');
INSERT INTO events (id, event, timestamp, message, performed_by)
VALUES (28, 'Termék eladva', 1724235048264,
        'Rendelésazonosító: 1 - Számlaazonosító: 1 | Mennyiség: 2, Fizetve: 1000, Termék: Somersby Apple',
        'Pultos1 (terminal)');
INSERT INTO events (id, event, timestamp, message, performed_by)
VALUES (29, 'Termék eladva', 1724235048275,
        'Rendelésazonosító: 1 - Számlaazonosító: 1 | Mennyiség: 1, Fizetve: 400, Termék: Sör', 'Pultos1 (terminal)');
INSERT INTO events (id, event, timestamp, message, performed_by)
VALUES (30, 'Fizetés', 1724235048279, '1: Horváth István - isti@simonyi.bme.hu | 200 JMF', 'Pultos1 (terminal)');
INSERT INTO events (id, event, timestamp, message, performed_by)
VALUES (31, 'Életciklus', 1724235211633, 'Az alkalmazás leállt', 'Rendszer');
INSERT INTO events (id, event, timestamp, message, performed_by)
VALUES (32, 'Principal létrehozva', 1724234495532, 'admin | Adminisztrátor', 'Ismeretlen végrehajtó');
INSERT INTO events (id, event, timestamp, message, performed_by)
VALUES (33, 'Principal létrehozva', 1724234714282, 'Pultos1 | Terminál', 'Isti (admin)');
INSERT INTO events (id, event, timestamp, message, performed_by)
VALUES (34, 'Életciklus', 1724235214161, 'Az alkalmazás elindult', 'Rendszer');
INSERT INTO events (id, event, timestamp, message, performed_by)
VALUES (35, 'Principal létrehozva', 1724235214248, 'admin | Adminisztrátor', 'Ismeretlen végrehajtó');
INSERT INTO events (id, event, timestamp, message, performed_by)
VALUES (36, 'Életciklus', 1724235234677, 'Az alkalmazás leállt', 'Rendszer');
INSERT INTO events (id, event, timestamp, message, performed_by)
VALUES (37, 'Principal létrehozva', 1724235214248, 'admin | Adminisztrátor', 'Ismeretlen végrehajtó');
INSERT INTO events (id, event, timestamp, message, performed_by)
VALUES (38, 'Életciklus', 1724235236319, 'Az alkalmazás elindult', 'Rendszer');
INSERT INTO events (id, event, timestamp, message, performed_by)
VALUES (39, 'Utalvány Létrehozva', 1724235309302, 'Számlaazonosító: 2, Termékazonosító: 3, Darabszám: 100',
        'Isti (admin)');
INSERT INTO events (id, event, timestamp, message, performed_by)
VALUES (40, 'Kártya hozzárendelve', 1724235667785, '1: Horváth István - isti@simonyi.bme.hu', 'Pultos1 (terminal)');
INSERT INTO events (id, event, timestamp, message, performed_by)
VALUES (41, 'Számla szerkesztve', 1724235667775, '2: Példa Petra - petra@gmail.com', 'Pultos1 (terminal)');
INSERT INTO events (id, event, timestamp, message, performed_by)
VALUES (42, 'Életciklus', 1724235775645, 'Az alkalmazás leállt', 'Rendszer');
INSERT INTO events (id, event, timestamp, message, performed_by)
VALUES (43, 'Életciklus', 1724235777370, 'Az alkalmazás elindult', 'Rendszer');
INSERT INTO events (id, event, timestamp, message, performed_by)
VALUES (44, 'Kártya hozzárendelve', 1724235815232, '2: Példa Petra - petra@gmail.com', 'Pultos1 (terminal)');
INSERT INTO events (id, event, timestamp, message, performed_by)
VALUES (45, 'Utalvány Létrehozva', 1724235873438, 'Számlaazonosító: 1, Termékazonosító: 1, Darabszám: 2',
        'Isti (admin)');
INSERT INTO events (id, event, timestamp, message, performed_by)
VALUES (46, 'Utalvány beváltva', 1724235886179, 'Rendelésazonosító: 15 - Számlaazonosító: 1', 'Pultos1 (terminal)');
INSERT INTO events (id, event, timestamp, message, performed_by)
VALUES (47, 'Utalvány módosítva', 1724235886171, 'Számlaazonosító: 1, Termékazonosító: 1, Darabszám: 1',
        'Pultos1 (terminal)');
INSERT INTO events (id, event, timestamp, message, performed_by)
VALUES (48, 'Rendelés létrehozva', 1724235886161, 'Rendelésazonosító: 15 - Számlaazonosító: 1', 'Pultos1 (terminal)');
INSERT INTO events (id, event, timestamp, message, performed_by)
VALUES (49, 'Rendelés létrehozva', 1724235934318, 'Rendelésazonosító: 18 - Számlaazonosító: 1', 'Pultos1 (terminal)');
INSERT INTO events (id, event, timestamp, message, performed_by)
VALUES (50, 'Termék eladva', 1724235934324,
        'Rendelésazonosító: 18 - Számlaazonosító: 1 | Mennyiség: 1, Fizetve: 400, Termék: Sör', 'Pultos1 (terminal)');
INSERT INTO events (id, event, timestamp, message, performed_by)
VALUES (51, 'Feltöltés', 1724235952657, '2: Példa Petra - petra@gmail.com | 5000 JMF', 'Pultos1 (terminal)');
INSERT INTO events (id, event, timestamp, message, performed_by)
VALUES (52, 'Feltöltés', 1724235957881, '1: Horváth István - isti@simonyi.bme.hu | 5000 JMF', 'Pultos1 (terminal)');
INSERT INTO events (id, event, timestamp, message, performed_by)
VALUES (53, 'Fizetés', 1724235972338, '2: Példa Petra - petra@gmail.com | 1500 JMF', 'Pultos1 (terminal)');

select setval(pg_get_serial_sequence('accounts', 'id'), (select max(id) from accounts));
select setval(pg_get_serial_sequence('events', 'id'), (select max(id) from events));
select setval(pg_get_serial_sequence('principals', 'id'), (select max(id) from principals));
select setval(pg_get_serial_sequence('items', 'id'), (select max(id) from items));
select setval(pg_get_serial_sequence('orders', 'id'), (select max(id) from orders));
select setval(pg_get_serial_sequence('order_lines', 'id'), (select max(id) from order_lines));
select setval(pg_get_serial_sequence('vouchers', 'id'), (select max(id) from vouchers));
select setval(pg_get_serial_sequence('transactions', 'id'), (select max(id) from transactions));
