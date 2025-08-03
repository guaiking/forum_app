const express = require('express');
const bodyParser = require('body-parser');
const cors = require('cors');
const sqlite3 = require('sqlite3').verbose();

const app = express();
const PORT = 3000;

// 中间件配置（关键修复）
app.use(cors());
app.use(bodyParser.json()); // 解析application/json
app.use(bodyParser.urlencoded({ extended: true })); // 解析application/x-www-form-urlencoded

app.use(cors({
    origin: true, // 或指定您的域名
    credentials: true,
    methods: ['GET', 'POST', 'PUT', 'DELETE', 'OPTIONS'],
    allowedHeaders: ['Content-Type', 'Authorization']
}));

// 数据库连接
const db = new sqlite3.Database('./auth.db');

// 创建用户表
db.serialize(() => {
    db.run(`CREATE TABLE IF NOT EXISTS users (
        id INTEGER PRIMARY KEY AUTOINCREMENT,
        username TEXT UNIQUE NOT NULL,
        password TEXT NOT NULL,
        created_at DATETIME DEFAULT CURRENT_TIMESTAMP
    )`);
});

db.serialize(() => {
    db.run(`CREATE TABLE IF NOT EXISTS conversations (
        id INTEGER PRIMARY KEY AUTOINCREMENT,
        user_id INTEGER NOT NULL,
        title TEXT NOT NULL,
        content TEXT NOT NULL,
        created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
        FOREIGN KEY(user_id) REFERENCES users(id)
    )`);
});

// 在index.js中添加以下表创建代码
db.serialize(() => {
    db.run(`CREATE TABLE IF NOT EXISTS posts (
        id INTEGER PRIMARY KEY AUTOINCREMENT,
        user_id INTEGER NOT NULL,
        title TEXT NOT NULL,
        content TEXT NOT NULL,
        created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
        FOREIGN KEY(user_id) REFERENCES users(id)
        )`);

    db.run(`CREATE TABLE IF NOT EXISTS user_profiles (
        user_id INTEGER PRIMARY KEY,
        nickname TEXT,
        avatar TEXT,
        FOREIGN KEY(user_id) REFERENCES users(id)
    )`);
});

// 登录接口（添加错误处理）
app.post('/api/login', (req, res) => {
    try {
        // 添加参数验证
        if (!req.body || !req.body.username || !req.body.password) {
            return res.json({
                status: 'error',
                message: '用户名和密码不能为空'
            });
        }

        const { username, password } = req.body;

        db.get('SELECT * FROM users WHERE username = ?', [username], (err, row) => {
            if (err) {
                console.error('数据库错误:', err);
                return res.json({
                    status: 'error',
                    message: '服务器内部错误'
                });
            }

            if (!row) {
                return res.json({
                    status: 'error',
                    message: '用户名不存在'
                });
            }

            if (row.password !== password) {
                return res.json({
                    status: 'error',
                    message: '密码错误'
                });
            }

            res.json({
                status: 'success',
                message: '登录成功',
                user: {
                    id: row.id,
                    username: row.username
                }
            });
        });
    } catch (error) {
        console.error('登录处理错误:', error);
        res.json({
            status: 'error',
            message: '服务器处理请求时出错'
        });
    }
});

// 注册接口（添加错误处理）
app.post('/api/register', (req, res) => {
    try {
        // 添加参数验证
        if (!req.body || !req.body.username || !req.body.password) {
            return res.json({
                status: 'error',
                message: '用户名和密码不能为空'
            });
        }

        const { username, password } = req.body;

        db.get('SELECT * FROM users WHERE username = ?', [username], (err, row) => {
            if (err) {
                console.error('数据库错误:', err);
                return res.json({
                    status: 'error',
                    message: '服务器内部错误'
                });
            }

            if (row) {
                return res.json({
                    status: 'error',
                    message: '用户名已存在'
                });
            }

            db.run('INSERT INTO users (username, password) VALUES (?, ?)',
                [username, password],
                function(err) {
                    if (err) {
                        console.error('注册失败:', err);
                        return res.json({
                            status: 'error',
                            message: '注册失败'
                        });
                    }

                    res.json({
                        status: 'success',
                        message: '注册成功',
                        userId: this.lastID
                    });
                });
        });
    } catch (error) {
        console.error('注册处理错误:', error);
        res.json({
            status: 'error',
            message: '服务器处理请求时出错'
        });
    }
});

// 添加保存对话的接口
app.post('/api/save_conversation', (req, res) => {
    try {
        const { userId, conversationId, title, content } = req.body;

        if (conversationId) {
            // 更新现有对话
            db.run('UPDATE conversations SET title = ?, content = ? WHERE id = ? AND user_id = ?',
                [title, content, conversationId, userId],
                function(err) {
                    if (err) {
                        console.error('更新对话失败:', err);
                        return res.json({
                            status: 'error',
                            message: '更新对话失败'
                        });
                    }

                    res.json({
                        status: 'success',
                        message: '对话更新成功',
                        conversationId: conversationId
                    });
                });
        } else {
            // 创建新对话
            db.run('INSERT INTO conversations (user_id, title, content) VALUES (?, ?, ?)',
                [userId, title, content],
                function(err) {
                    if (err) {
                        console.error('保存对话失败:', err);
                        return res.json({
                            status: 'error',
                            message: '保存对话失败'
                        });
                    }

                    res.json({
                        status: 'success',
                        message: '对话保存成功',
                        conversationId: this.lastID
                    });
                });
        }
    } catch (error) {
        console.error('保存对话处理错误:', error);
        res.json({
            status: 'error',
            message: '服务器处理请求时出错'
        });
    }
});

// 添加获取对话列表的接口
app.get('/api/conversations/:userId', (req, res) => {
    try {
        const userId = req.params.userId;

        db.all('SELECT id, title FROM conversations WHERE user_id = ? ORDER BY created_at DESC',
            [userId],
            (err, rows) => {
                if (err) {
                    console.error('获取对话列表失败:', err);
                    return res.json({
                        status: 'error',
                        message: '获取对话列表失败'
                    });
                }

                res.json({
                    status: 'success',
                    conversations: rows
                });
            });
    } catch (error) {
        console.error('获取对话列表处理错误:', error);
        res.json({
            status: 'error',
            message: '服务器处理请求时出错'
        });
    }
});

// 添加获取单个对话内容的接口
app.get('/api/conversation/:id', (req, res) => {
    try {
        const id = req.params.id;

        db.get('SELECT content FROM conversations WHERE id = ?',
            [id],
            (err, row) => {
                if (err) {
                    console.error('获取对话内容失败:', err);
                    return res.json({
                        status: 'error',
                        message: '获取对话内容失败'
                    });
                }

                if (!row) {
                    return res.json({
                        status: 'error',
                        message: '对话不存在'
                    });
                }

                res.json({
                    status: 'success',
                    content: row.content
                });
            });
    } catch (error) {
        console.error('获取对话内容处理错误:', error);
        res.json({
            status: 'error',
            message: '服务器处理请求时出错'
        });
    }
});

// 发布论坛帖子
app.post('/api/create_post', (req, res) => {
    try {
        const { userId, title, content } = req.body;

        if (!userId || !title || !content) {
            return res.json({
                status: 'error',
                message: '参数不完整'
            });
        }

        db.run('INSERT INTO posts (user_id, title, content) VALUES (?, ?, ?)',
            [userId, title, content],
            function(err) {
                if (err) {
                    console.error('发布帖子失败:', err);
                    return res.json({
                        status: 'error',
                        message: '发布帖子失败'
                    });
                }

                res.json({
                    status: 'success',
                    message: '帖子发布成功',
                    postId: this.lastID
                });
            });
    } catch (error) {
        console.error('发布帖子处理错误:', error);
        res.json({
            status: 'error',
            message: '服务器处理请求时出错'
        });
    }
});



// 获取所有帖子列表
app.get('/api/posts', (req, res) => {
    try {
        db.all(`
            SELECT p.id, p.title, p.created_at, u.username, up.nickname, up.avatar 
            FROM posts p 
            JOIN users u ON p.user_id = u.id 
            LEFT JOIN user_profiles up ON p.user_id = up.user_id
            ORDER BY p.created_at DESC`,
            (err, rows) => {
                if (err) {
                    console.error('获取帖子列表失败:', err);
                    return res.json({
                        status: 'error',
                        message: '获取帖子列表失败'
                    });
                }

                res.json({
                    status: 'success',
                    posts: rows
                });
            });
    } catch (error) {
        console.error('获取帖子列表处理错误:', error);
        res.json({
            status: 'error',
            message: '服务器处理请求时出错'
        });
    }
});

// 获取单个帖子内容
app.get('/api/post/:id', (req, res) => {
    try {
        const id = req.params.id;

        db.get(`
            SELECT p.*, u.username, up.nickname, up.avatar 
            FROM posts p 
            JOIN users u ON p.user_id = u.id 
            LEFT JOIN user_profiles up ON p.user_id = up.user_id
            WHERE p.id = ?`,
            [id],
            (err, row) => {
                if (err) {
                    console.error('获取帖子内容失败:', err);
                    return res.json({
                        status: 'error',
                        message: '获取帖子内容失败'
                    });
                }

                if (!row) {
                    return res.json({
                        status: 'error',
                        message: '帖子不存在'
                    });
                }

                res.json({
                    status: 'success',
                    post: row
                });
            });
    } catch (error) {
        console.error('获取帖子内容处理错误:', error);
        res.json({
            status: 'error',
            message: '服务器处理请求时出错'
        });
    }
});

// 获取用户个人帖子
app.get('/api/user_posts/:userId', (req, res) => {
    try {
        const userId = req.params.userId;

        db.all(`
            SELECT p.id, p.title, p.created_at 
            FROM posts p 
            WHERE p.user_id = ? 
            ORDER BY p.created_at DESC`,
            [userId],
            (err, rows) => {
                if (err) {
                    console.error('获取用户帖子失败:', err);
                    return res.json({
                        status: 'error',
                        message: '获取用户帖子失败'
                    });
                }

                res.json({
                    status: 'success',
                    posts: rows
                });
            });
    } catch (error) {
        console.error('获取用户帖子处理错误:', error);
        res.json({
            status: 'error',
            message: '服务器处理请求时出错'
        });
    }
});

// 更新用户资料
app.post('/api/update_profile', (req, res) => {
    try {
        const { userId, nickname, avatar } = req.body;

        db.run(`
            INSERT OR REPLACE INTO user_profiles (user_id, nickname, avatar) 
            VALUES (?, ?, ?)`,
            [userId, nickname, avatar],
            function(err) {
                if (err) {
                    console.error('更新资料失败:', err);
                    return res.json({
                        status: 'error',
                        message: '更新资料失败'
                    });
                }

                res.json({
                    status: 'success',
                    message: '资料更新成功'
                });
            });
    } catch (error) {
        console.error('更新资料处理错误:', error);
        res.json({
            status: 'error',
            message: '服务器处理请求时出错'
        });
    }
});

// 获取用户资料
app.get('/api/profile/:userId', (req, res) => {
    try {
        const userId = req.params.userId;

        db.get(`
            SELECT u.username, up.nickname, up.avatar 
            FROM users u 
            LEFT JOIN user_profiles up ON u.id = up.user_id
            WHERE u.id = ?`,
            [userId],
            (err, row) => {
                if (err) {
                    console.error('获取用户资料失败:', err);
                    return res.json({
                        status: 'error',
                        message: '获取用户资料失败'
                    });
                }

                res.json({
                    status: 'success',
                    profile: row || { username: '', nickname: '', avatar: '' }
                });
            });
    } catch (error) {
        console.error('获取用户资料处理错误:', error);
        res.json({
            status: 'error',
            message: '服务器处理请求时出错'
        });
    }
});







// 管理员获取用户列表
app.get('/api/admin/users', (req, res) => {
    try {
        const page = parseInt(req.query.page) || 1;
        const limit = parseInt(req.query.limit) || 10;
        const offset = (page - 1) * limit;
        const search = req.query.search || '';

        let query = `
            SELECT
                u.id, u.username, u.created_at,
                up.nickname, up.avatar,
                (SELECT COUNT(*) FROM posts p WHERE p.user_id = u.id) AS post_count,
                (SELECT COUNT(*) FROM conversations c WHERE c.user_id = u.id) AS conversation_count
            FROM users u
                     LEFT JOIN user_profiles up ON u.id = up.user_id
        `;

        let countQuery = 'SELECT COUNT(*) AS total FROM users u';

        const params = [];

        if (search) {
            query += ` WHERE u.username LIKE ? OR up.nickname LIKE ?`;
            countQuery += ` WHERE u.username LIKE ? OR up.nickname LIKE ?`;
            params.push(`%${search}%`, `%${search}%`);
        }

        query += ` LIMIT ? OFFSET ?`;
        params.push(limit, offset);

        db.serialize(() => {
            db.get(countQuery, params.slice(0, search ? 2 : 0), (err, countRow) => {
                if (err) {
                    console.error('获取用户总数失败:', err);
                    return res.status(500).json({ error: '获取用户总数失败' });
                }

                db.all(query, params, (err, rows) => {
                    if (err) {
                        console.error('获取用户列表失败:', err);
                        return res.status(500).json({ error: '获取用户列表失败' });
                    }

                    res.json({
                        users: rows,
                        total: countRow.total,
                        page,
                        limit
                    });
                });
            });
        });
    } catch (error) {
        console.error('管理员获取用户列表错误:', error);
        res.status(500).json({ error: '服务器处理请求时出错' });
    }
});

// 管理员获取单个用户详情
app.get('/api/admin/users/:id', (req, res) => {
    try {
        const userId = req.params.id;

        db.serialize(() => {
            // 获取用户基本信息
            db.get(`
                SELECT
                    u.id, u.username, u.created_at,
                    up.nickname, up.avatar
                FROM users u
                         LEFT JOIN user_profiles up ON u.id = up.user_id
                WHERE u.id = ?
            `, [userId], (err, user) => {
                if (err) {
                    console.error('获取用户基本信息失败:', err);
                    return res.status(500).json({ error: '获取用户基本信息失败' });
                }

                if (!user) {
                    return res.status(404).json({ error: '用户不存在' });
                }

                // 获取用户帖子
                db.all(`
                    SELECT id, title, content, created_at
                    FROM posts
                    WHERE user_id = ?
                    ORDER BY created_at DESC
                `, [userId], (err, posts) => {
                    if (err) {
                        console.error('获取用户帖子失败:', err);
                        return res.status(500).json({ error: '获取用户帖子失败' });
                    }

                    // 获取用户对话
                    db.all(`
                        SELECT id, title, content, created_at
                        FROM conversations
                        WHERE user_id = ?
                        ORDER BY created_at DESC
                    `, [userId], (err, conversations) => {
                        if (err) {
                            console.error('获取用户对话失败:', err);
                            return res.status(500).json({ error: '获取用户对话失败' });
                        }

                        res.json({
                            ...user,
                            posts: posts || [],
                            conversations: conversations || []
                        });
                    });
                });
            });
        });
    } catch (error) {
        console.error('管理员获取用户详情错误:', error);
        res.status(500).json({ error: '服务器处理请求时出错' });
    }
});

// 管理员获取帖子列表
app.get('/api/admin/posts', (req, res) => {
    try {
        const page = parseInt(req.query.page) || 1;
        const limit = parseInt(req.query.limit) || 10;
        const offset = (page - 1) * limit;
        const search = req.query.search || '';

        let query = `
            SELECT 
                p.id, p.title, p.created_at,
                u.username,
                up.nickname
            FROM posts p
            JOIN users u ON p.user_id = u.id
            LEFT JOIN user_profiles up ON p.user_id = up.user_id
        `;

        let countQuery = `
            SELECT COUNT(*) AS total 
            FROM posts p
            JOIN users u ON p.user_id = u.id
            LEFT JOIN user_profiles up ON p.user_id = up.user_id
        `;

        const params = [];

        if (search) {
            query += ` WHERE p.title LIKE ? OR p.content LIKE ? OR u.username LIKE ? OR up.nickname LIKE ?`;
            countQuery += ` WHERE p.title LIKE ? OR p.content LIKE ? OR u.username LIKE ? OR up.nickname LIKE ?`;
            params.push(
                `%${search}%`, `%${search}%`, `%${search}%`, `%${search}%`
            );
        }

        query += ` ORDER BY p.created_at DESC LIMIT ? OFFSET ?`;
        params.push(limit, offset);

        db.serialize(() => {
            db.get(countQuery, params.slice(0, search ? 4 : 0), (err, countRow) => {
                if (err) {
                    console.error('获取帖子总数失败:', err);
                    return res.status(500).json({ error: '获取帖子总数失败' });
                }

                db.all(query, params, (err, rows) => {
                    if (err) {
                        console.error('获取帖子列表失败:', err);
                        return res.status(500).json({ error: '获取帖子列表失败' });
                    }

                    res.json({
                        posts: rows,
                        total: countRow.total,
                        page,
                        limit
                    });
                });
            });
        });
    } catch (error) {
        console.error('管理员获取帖子列表错误:', error);
        res.status(500).json({ error: '服务器处理请求时出错' });
    }
});

// 管理员获取单个帖子详情
app.get('/api/admin/posts/:id', (req, res) => {
    try {
        const postId = req.params.id;

        db.get(`
            SELECT 
                p.*,
                u.username,
                up.nickname
            FROM posts p
            JOIN users u ON p.user_id = u.id
            LEFT JOIN user_profiles up ON p.user_id = up.user_id
            WHERE p.id = ?
        `, [postId], (err, post) => {
            if (err) {
                console.error('获取帖子详情失败:', err);
                return res.status(500).json({ error: '获取帖子详情失败' });
            }

            if (!post) {
                return res.status(404).json({ error: '帖子不存在' });
            }

            res.json(post);
        });
    } catch (error) {
        console.error('管理员获取帖子详情错误:', error);
        res.status(500).json({ error: '服务器处理请求时出错' });
    }
});

// 管理员获取对话列表
app.get('/api/admin/conversations', (req, res) => {
    try {
        const page = parseInt(req.query.page) || 1;
        const limit = parseInt(req.query.limit) || 10;
        const offset = (page - 1) * limit;
        const search = req.query.search || '';

        let query = `
            SELECT 
                c.id, c.title, c.created_at,
                u.username,
                up.nickname
            FROM conversations c
            JOIN users u ON c.user_id = u.id
            LEFT JOIN user_profiles up ON c.user_id = up.user_id
        `;

        let countQuery = `
            SELECT COUNT(*) AS total 
            FROM conversations c
            JOIN users u ON c.user_id = u.id
            LEFT JOIN user_profiles up ON c.user_id = up.user_id
        `;

        const params = [];

        if (search) {
            query += ` WHERE c.title LIKE ? OR c.content LIKE ? OR u.username LIKE ? OR up.nickname LIKE ?`;
            countQuery += ` WHERE c.title LIKE ? OR c.content LIKE ? OR u.username LIKE ? OR up.nickname LIKE ?`;
            params.push(
                `%${search}%`, `%${search}%`, `%${search}%`, `%${search}%`
            );
        }

        query += ` ORDER BY c.created_at DESC LIMIT ? OFFSET ?`;
        params.push(limit, offset);

        db.serialize(() => {
            db.get(countQuery, params.slice(0, search ? 4 : 0), (err, countRow) => {
                if (err) {
                    console.error('获取对话总数失败:', err);
                    return res.status(500).json({ error: '获取对话总数失败' });
                }

                db.all(query, params, (err, rows) => {
                    if (err) {
                        console.error('获取对话列表失败:', err);
                        return res.status(500).json({ error: '获取对话列表失败' });
                    }

                    res.json({
                        conversations: rows,
                        total: countRow.total,
                        page,
                        limit
                    });
                });
            });
        });
    } catch (error) {
        console.error('管理员获取对话列表错误:', error);
        res.status(500).json({ error: '服务器处理请求时出错' });
    }
});

// 管理员获取单个对话详情
app.get('/api/admin/conversations/:id', (req, res) => {
    try {
        const conversationId = req.params.id;

        db.get(`
            SELECT 
                c.*,
                u.username,
                up.nickname
            FROM conversations c
            JOIN users u ON c.user_id = u.id
            LEFT JOIN user_profiles up ON c.user_id = up.user_id
            WHERE c.id = ?
        `, [conversationId], (err, conversation) => {
            if (err) {
                console.error('获取对话详情失败:', err);
                return res.status(500).json({ error: '获取对话详情失败' });
            }

            if (!conversation) {
                return res.status(404).json({ error: '对话不存在' });
            }

            res.json(conversation);
        });
    } catch (error) {
        console.error('管理员获取对话详情错误:', error);
        res.status(500).json({ error: '服务器处理请求时出错' });
    }
});



app.listen(PORT, '0.0.0.0', () => {
    console.log(`服务器运行在 http://192.168.43.143:${PORT}`);
});