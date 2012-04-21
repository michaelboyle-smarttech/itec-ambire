package org.eun.itec.ambire.teacher;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;

import javax.servlet.ServletContext;

public class SqlDataProvider implements DataProvider {


	private String CONNECTION_STRING;
	private String USERNAME;
	private String PASSWORD;
	private static boolean g_prepared = false;
	private static boolean g_firstRun = true;
	
	public SqlDataProvider(ServletContext context) {
		CONNECTION_STRING = context.getInitParameter("SqlDataProvider.CONNECTION_STRING");
		if(CONNECTION_STRING == null) {
			CONNECTION_STRING = "jdbc:mysql://localhost:3306/itecambire";
		}
		USERNAME = context.getInitParameter("SqlDataProvider.USERNAME");
		if(USERNAME == null) {
			USERNAME = "itecambire";
		}
		PASSWORD = context.getInitParameter("SqlDataProvider.PASSWORD");
		if(PASSWORD == null) {
			PASSWORD = "WP3.taik.fi";
		}
		prepareDatabase();
	}
	
	public void close() {
		g_prepared = false;
		g_firstRun = false;
	}
	
	private Connection open() {
		if(!g_prepared && !g_firstRun) {
			return null;
		}
		Properties props = new Properties();
		props.setProperty("user", USERNAME);
		props.setProperty("password", PASSWORD);
		try {
			if(g_firstRun) {
				g_firstRun = false;
				Class.forName("com.mysql.jdbc.Driver").newInstance();
			}
			return DriverManager.getConnection(CONNECTION_STRING, props);
		} catch (Exception e) {
			return null;
		}
	}
	
	private void close(Connection db) {
		if(db != null) {
			try {
				db.close();
			} catch(SQLException e) {}
		}
	}
	
	private void close(PreparedStatement st) {
		if(st != null) {
			try {
				st.close();
			} catch(SQLException e) {}
		}
	}
	
	private void close(ResultSet rs) {
		if(rs != null) {
			try {
				rs.close();
			} catch(SQLException e) {}
		}
	}
	
	private void close(Statement st) {
		if(st != null) {
			try {
				st.close();
			} catch(SQLException e) {}
		}
	}
	private void prepareDatabase() {
		if(!g_prepared) {
			boolean sessionsExists = false;
			boolean uploadsExists = false;
			try {
				Connection db = open();
				try {
					Statement st = db.createStatement();
					try {
						ResultSet rs = st.executeQuery("SELECT COUNT(*) FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_NAME = \'Sessions\'");
						try {
							while(rs.next()) {
								int count = rs.getInt(1);
								if(count > 0) {
									sessionsExists = true;
								}
								break;
							}
						} finally {
							close(rs);
						}
						if(!sessionsExists) {
							st.execute("CREATE TABLE Sessions (\n" 
				                + "sessionId BIGINT AUTO_INCREMENT PRIMARY KEY,\n"
				                + "pin NVARCHAR(10) NOT NULL,\n"
				                + "owner NVARCHAR(90) NOT NULL,\n"
				                + "timestamp FLOAT NOT NULL)");
							sessionsExists = true;
							st.execute(String.format("INSERT INTO Sessions(pin,owner,timestamp) VALUES(\'8675309\',\'michaelboyle.smarttech@gmail.com\',%d)", System.currentTimeMillis() + 24 * 60 * 60 * 1000));
						}
						rs = st.executeQuery("SELECT COUNT(*) FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_NAME = \'Uploads\'");
						try {
							while(rs.next()) {
								int count = rs.getInt(1);
								if(count > 0) {
									uploadsExists = true;
								}
								break;
							}
						} finally {
							close(rs);
						}					
						if(!uploadsExists) {
							st.execute("CREATE TABLE Uploads (\n"
								+ "uploadId BIGINT AUTO_INCREMENT PRIMARY KEY,\n"
								+ "sessionId BIGINT NOT NULL,\n"
								+ "href NVARCHAR(400) NOT NULL,\n"
								+ "token NVARCHAR(200) NOT NULL,\n"
								+ "name NVARCHAR(140) NOT NULL,\n"
								+ "kind NVARCHAR(40) NOT NULL,\n"
								+ "mimeType NVARCHAR(60) NOT NULL,\n"
								+ "timestamp FLOAT NOT NULL,\n"
								+ "width INT NOT NULL,\n"
								+ "height INT NOT NULL)");
							uploadsExists = true;
						}
					} finally {
						close(st);
					}
				} finally {
					close(db);
				}
			} catch(Exception e) {
			}
			g_prepared = sessionsExists && uploadsExists;
		}
	}
	
	@Override
	public double verify(String pin) {
		double sessionId = Double.NaN;
		try {
			Connection db = open();
			try {
				PreparedStatement st = db.prepareStatement("SELECT sessionId FROM Sessions WHERE pin = ?");
				try {
					st.setString(1, pin);
					ResultSet rs = st.executeQuery();
					try {
						while(rs.next()) {
							sessionId = (double)rs.getLong(1);
							break;
						}
					} finally {
						close(rs);
					}
				} finally {
					close(st);
				}
			} finally {
				close(db);
			}
		} catch(Exception e) {}
		return sessionId;
	}

	@Override
	public String selectPin(String owner) {
		String pin = null;
		try {
			Connection db = open();
			try {
				PreparedStatement st = db.prepareStatement("SELECT pin FROM Sessions WHERE owner = ?");
				try {
					st.setString(1, owner);
					ResultSet rs = st.executeQuery();
					try {
						while(rs.next()) {
							pin = rs.getString(1);
							break;
						}
					} finally {
						close(rs);
					}
				} finally {
					close(st);
				}
				for(int seed = 1; true; ++seed) {
					String suggestion = Deployment.suggestPin(owner, seed);
					st = db.prepareStatement("SELECT COUNT(*) FROM Sessions WHERE pin = ?");
					int count = 0;
					try {
						st.setString(1, suggestion);
						ResultSet rs = st.executeQuery();
						try {
							while(rs.next()) {
								count = rs.getInt(1);
							}
						} finally {
							close(rs);
						}
					} finally {
						close(st);
					}
					if(count == 0) {
						pin = suggestion;
						break;
					}
				}
			} finally {
				close(db);
			}
		} catch(Exception e) {}
		return pin;
	}

	@Override
	public double signIn(String owner, String pin, double timestamp) {
		double sessionId = Double.NaN;
		try {
			Connection db = open();
			try {
				PreparedStatement st = db.prepareStatement("INSERT INTO Sessions(owner,pin,timestamp) VALUES(?,?,?)");
				try {
					st.setString(1,owner);
					st.setString(2,pin);
					st.setDouble(3, timestamp);
					st.execute();
				} finally {
					close(st);
				}
				st = db.prepareStatement("SELECT @@IDENTITY");
				try {
					ResultSet rs = st.executeQuery();
					try {
						while(rs.next()) {
							sessionId = (double)rs.getLong(1);
							break;
						}
					} finally {
						close(rs);
					}
				} finally {
					close(st);
				}
			} finally {
				close(db);
			}
		} catch(Exception e) {
			e.printStackTrace();
		}
		return sessionId;
	}

	@Override
	public void signOut(double sessionId) {
		try {
			Connection db = open();
			try {
				PreparedStatement st = db.prepareStatement("DELETE FROM Sessions WHERE sessionId = ?");
				try {
					st.setLong(1, (long)sessionId);
					st.execute();
				} finally {
					close(st);
				}
                LinkedList<String> tokens = new LinkedList<String>();
                st = db.prepareStatement("SELECT token FROM Uploads WHERE sessionId = ?");
                try {
                	st.setLong(1, (long)sessionId);
                	ResultSet rs = st.executeQuery();
                	try {
                		while(rs.next()) {
                			tokens.add(rs.getString(1));
                		}
                	} finally {
                		close(rs);
                	}
                } finally {
                	close(st);
                }
                st = db.prepareStatement("DELETE FROM Uploads WHERE sessionId = ?");
                try {
                	st.setLong(1, (long)sessionId);
                	st.execute();
                } finally {
                	close(st);
                }
                Deployment.STORAGE_PROVIDER.deleteFiles(tokens, true);
			} finally {
				close(db);
			}
		} catch(Exception e) {}
	}

	@Override
	public void keepSignedIn(double sessionId, double timestamp) {
		try {
			Connection db = open();
			try {
				PreparedStatement st = db.prepareStatement("UPDATE Sessions SET timestamp = ? WHERE sessionId = ?");
				try {
					st.setDouble(1, timestamp);
					st.setDouble(2, sessionId);
					st.execute();
				} finally {
					close(st);
				}
			} finally {
				close(db);
			}
		} catch(Exception e) {
			e.printStackTrace();
		}
	}
	
	private static String left(String s, int len) {
		if(s.length() <= len) {
			return s;
		} else {
			return s.substring(0, len);
		}
	}
	@Override
	public double upload(double sessionId, String href, String token, String name, String kind, String mimeType, double timestamp, int width, int height, boolean replace) {
		double uploadId = Double.NaN;
		href = left(href, 400);
		token = left(token, 200);
		name = left(name, 140);
		kind = left(kind, 40);
		mimeType = left(mimeType, 60);
		try {
			Connection db = open();
			try {
	            if(replace) {
	                LinkedList<String> tokens = new LinkedList<String>();
	                PreparedStatement st = db.prepareStatement("SELECT token FROM Uploads WHERE sessionId = ? AND kind = ?");
	                try {
	                	st.setLong(1, (long)sessionId);
	                	st.setString(2, kind);
	                	ResultSet rs = st.executeQuery();
	                	try {
	                		while(rs.next()) {
	                			tokens.add(rs.getString(1));
	                		}
	                	} finally {
	                		close(rs);
	                	}
	                } finally {
	                	close(st);
	                }
	                st = db.prepareStatement("DELETE FROM Uploads WHERE sessionId = ? AND kind = ?");
	                try {
	                	st.setLong(1, (long)sessionId);
	                	st.setString(2, kind);
	                	st.execute();
	                } finally {
	                	close(st);
	                }
	                Deployment.STORAGE_PROVIDER.deleteFiles(tokens, false);
	            }
	            PreparedStatement st = db.prepareStatement("INSERT INTO Uploads(sessionId,href,token,name,kind,mimeType,timestamp,width,height) VALUES(?,?,?,?,?,?,?,?,?)");
	            try {
	            	st.setLong(1, (long)sessionId);
	            	st.setString(2,href);
	            	st.setString(3,token);
	            	st.setString(4,name);
	            	st.setString(5,kind);
	            	st.setString(6,mimeType);
	            	st.setDouble(7,timestamp);
	            	st.setInt(8,width);
	            	st.setInt(9,height);
	            	st.execute();
	            } finally {
	            	close(st);
	            }
	            st = db.prepareStatement("SELECT @@IDENTITY");
	            try {
	            	ResultSet rs = st.executeQuery();
	            	try {
	            		while(rs.next()) {
	            			uploadId = (double)rs.getLong(1);
	            			break;
	            		}
	            	} finally {
	            		close(rs);
	            	}
	            } finally {
	            	close(st);
	            }
			} finally {
				close(db);
			}
		} catch(Exception e) {}
		return uploadId;
	}

	@Override
	public List<UploadInfo> selectUploads(double sessionId, double sinceTimestamp) {
		LinkedList<UploadInfo> list = new LinkedList<UploadInfo>();
		if(Double.isNaN(sinceTimestamp)) {
			sinceTimestamp = 0;
		}
		try {
			Connection db = open();
			try {
				PreparedStatement st = db.prepareStatement("SELECT uploadId,href,name,kind,timestamp,width,height FROM Uploads WHERE sessionId = ? AND timestamp > ?");
				try {
					st.setLong(1, (long)sessionId);
					st.setDouble(2, sinceTimestamp);
					ResultSet rs = st.executeQuery();
					try {
						while(rs.next()) {
							UploadInfo u = new UploadInfo();
							u.uploadId = rs.getLong(1);
							u.href = rs.getString(2);
							u.name = rs.getString(3);
							u.kind = rs.getString(4);
							u.timestamp = rs.getDouble(5);
							u.width = rs.getInt(6);
							u.height = rs.getInt(7);
							list.add(u);
						}
					} finally {
						close(rs);
					}
				} finally {
					close(st);
				}
			} finally {
				close(db);
			}
		} catch(Exception e) {}
		return list;
	}

	@Override
	public void collectGarbage() {
		double now = (double)System.currentTimeMillis();
		LinkedList<String> tokens = new LinkedList<String>();
		try {
			Connection db = open();
			try {
				PreparedStatement st = db.prepareStatement("UPDATE Uploads SET timestamp = ? WHERE sessionId IN (SELECT S.sessionId FROM Sessions S WHERE S.timestamp < ?)");
				try {
					st.setDouble(1, 0);
					st.setDouble(2, now - Deployment.MAX_SESSION_AGE_MILLIS);
					st.execute();
				} finally {
					close(st);
				}
				st = db.prepareStatement("SELECT token FROM Uploads WHERE timestamp <= ?");
				try {
					st.setDouble(1, now - Deployment.MAX_UPLOAD_AGE_MILLIS);
					ResultSet rs = st.executeQuery();
					try {
						while(rs.next()) {
							tokens.add(rs.getString(1));
						}
					} finally {
						close(rs);
					}
				} finally {
					close(st);
				}
				st = db.prepareStatement("DELETE FROM Uploads WHERE timestamp <= ?");
				try {
					st.setDouble(1, now - Deployment.MAX_UPLOAD_AGE_MILLIS);
					st.execute();
				} finally {
					close(st);
				}
				st = db.prepareStatement("DELETE FROM Sessions WHERE timestamp <= ?");
				try {
					st.setDouble(1, now - Deployment.MAX_SESSION_AGE_MILLIS);
					st.execute();
				} finally {
					close(st);
				}
			} finally {
				close(db);
			}
		} catch(Exception e) {
			e.printStackTrace();
		}
		Deployment.STORAGE_PROVIDER.deleteFiles(tokens, false);
	}
}
