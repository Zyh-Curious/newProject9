package org.example;

public class UserManagerTest {
    public static void main(String[] args) {
        // 1. 新增
        UserManager.User u = new UserManager.User(0, "test_user");
        System.out.println("add: " + UserManager.addUser(u));

        // 2. 查询（假设新增返回 id，这里手动指定一个已知 id）
        UserManager.User got = UserManager.getUser(1);
        System.out.println("get: " + got);

        // 3. 更新
        System.out.println("update: " + UserManager.updateUser(new UserManager.User(1, "new_name")));

        // 4. 删除
        System.out.println("remove: " + UserManager.removeUser(1));
    }
}
